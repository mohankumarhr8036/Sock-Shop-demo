import boto3
import datetime
import time
import logging
import sys
import re
import os
#import awstest_ami

OSLevelCommandslinux = ["netstat -ap","lsof"]
OSLevelCommandsWindows = ["Get-NetTCPConnection"]
DocumentNameLinux = 'AWS-RunShellScript'
DocumentNameWindows = 'AWS-RunPowerShellScript'
SSM_DRAIN_TIME = 10
RUNIDPREFIX = str(datetime.datetime.utcnow().timestamp())+"/"
BUCKET_NAME = 'threat-list'

def instanceid_is_valid(instance_id):
    if re.match('^i-[a-z0-9]{17}$', instance_id):
        logging.debug('Instance ID is in a valid format. ' + instance_id)
        return True
    else:
        logging.error(
            "Instance ID \"%s\" is invalid. Must enter valid EC2 Instance ID, e.g.: \"i-1a2b3c4d678\"" % instance_id)
        sys.exit()

def metadata(ec2Client, instance_id):
    global amiId
    print("Preparing instance for metadata information")

    try:
        instance_describe_metadata = ec2Client.describe_instances(
            InstanceIds=[
                instance_id
            ],
        )
    except ValueError as e:
        message = 'Unable to get instance metadata' + str(e['ErrorMessage'])

    target_instance_data = instance_describe_metadata['Reservations'][0]['Instances'][0]

    amiId = instance_describe_metadata['Reservations'][0]['Instances'][0]['ImageId']

    # Log and upload instance metadata to S3
    print(target_instance_data)
    ################################################################################
    metadata_file = 'metadata_file-' + instance_id + '.output'
    with open(metadata_file, 'w') as f:
        f.write(str(target_instance_data))
        f.close()
    data = open(metadata_file, 'rb')

    upload_to_s3(data, metadata_file)

def ami_platform(ec2Client, amiId):
    response = ec2Client.describe_images(
        Filters=[
            {
                'Name': 'image-id',
                'Values': [
                    amiId,
                ]
            },
        ]
    )
    Images = response['Images']
    for i in range(len(Images)):
        platform = Images[i]['PlatformDetails']
    #print(platform)
    return platform

def upload_to_s3(data, filename):
    try:
        s3 = boto3.resource('s3')
        response = s3.Bucket(BUCKET_NAME).put_object(Key=RUNIDPREFIX+filename, Body=data, ServerSideEncryption='AES256', ACL='bucket-owner-full-control')
        message = "Successfully uploaded file to bucket " + BUCKET_NAME
    except ValueError as e:
        message = "Unable to upload file to s3 bucket" + str(e['ErrorMessage'])
    print(message)

def set_termination_protection(ec2Client, instance_id):
    print("Setting termination protection for the instance")
    try:
        response = ec2Client.modify_instance_attribute(
            InstanceId=instance_id,
            DisableApiTermination={
                'Value': True
            }
        )
        print(response)
        message = "Termination protection enabled for instance" + instance_id
    except ValueError as e:
        message = "Unable to set Termination protection for instance" + instance_id + str(e['ErrorMessage'])

    print(message)

def remove_EC2_IAM_role(ec2Client, instance_id):
    try:
        thisIAMAssociationIDInfo = ec2Client.describe_iam_instance_profile_associations(Filters=[{'Name': 'instance-id', 'Values': [instance_id]}])

        if len(thisIAMAssociationIDInfo['IamInstanceProfileAssociations']) == 0:
            message = 'No IAM instance profile attached to instance ' + instance_id
        else:
            thisIAMAssociationID = thisIAMAssociationIDInfo['IamInstanceProfileAssociations'][0]['AssociationId']
            ec2Client.disassociate_iam_instance_profile(AssociationId=thisIAMAssociationID)
            message = 'Successful IAM role removal for ' + instance_id
    except ValueError as e:
        message = "Unable to remove IAM role correctly --  " + instance_id + str(e['ErrorMessage'])

    print(message)

def is_instance_managed_by_SSM(ssm, instance_id):
    #first, we'll determine if the instance is under the influence of SSM to begin:
    #This can be optimized by converting this list of dicts into a more expected dict of instance IDs and then simply finding the key. Requires pandas likely.
    ssmInstanceInformation = ssm.describe_instance_information()
    for thisInstanceInfo in ssmInstanceInformation['InstanceInformationList']:
        if thisInstanceInfo['InstanceId'] == instance_id:
            print("Found instance " + instance_id + " is managed by SSM")
            return True
    #If we can't find this instance in the list that SSM knows about, then don't execute the send commands portion.
    print("Instance " + instance_id + " not found to be controlled by SSM.")
    return False

def attach_EC2_SSM_execution_IAM_role(ec2Client, instance_id):
    try:
        ec2Client.associate_iam_instance_profile(InstanceId=instance_id, IamInstanceProfile={'Arn': 'arn:aws:iam::057796347550:instance-profile/ec2roleforssm' , 'Name': 'ec2roleforssm'})
        message = "IAM instance profile successfully attached --  " + instance_id
    except ValueError as e:
        message = "Unable to attach IAM role correctly --  " + instance_id + str(e['ErrorMessage'])
    print(message)


# Sending SSM Run Command to the instance
def ssm_send_commands(ssm, instance_id, OSLevelCommands, documentname):
    try:
        #Add a pause here for a few seconds to allow the instance profile change to iron itself out, or the S3 upload can fail:
        time.sleep(5)

        #ACCOUNT_ID = boto3.client('sts').get_caller_identity().get('Account')
        response = ssm.send_command(
            InstanceIds=[
                instance_id
            ],
            DocumentName=documentname,
            TimeoutSeconds=240,

            Parameters={
                'commands':OSLevelCommands,
                'executionTimeout':['3600'],
                'workingDirectory':['/tmp']
            },
            OutputS3BucketName=BUCKET_NAME,
            OutputS3KeyPrefix=RUNIDPREFIX+'ssm-output-file',
            #ServiceRoleArn='arn:aws:iam::' + ACCOUNT_ID + ':role/' + SSMIAMRoleForSNS,
            #NotificationConfig={
                #'NotificationArn': SNS_TOPIC,
                #'NotificationEvents': [
                #    'Success', 'TimedOut', 'Cancelled', 'Failed',
               # ],
               # 'NotificationType': 'Invocation',
            #}
        )
    except ValueError as e:
        message = "Executing SSM Run command failed on instance " + instance_id + str(e['ErrorMessage'])

    # Polling SSM for completion of commands execution. We will wait for 60 seconds then move on
    count = 0
    maxcount = 20
    while (count < maxcount):
        status = (ssm.list_commands(CommandId=response['Command']['CommandId']))['Commands'][0]['Status']
        print("Waiting for SSM to return Success (" + str(count) + " of " + str(maxcount) + " retries) -- SSM status is: " + status)
        if status == 'Pending' or status == 'InProgress':
            time.sleep(3)
            count += 1
        else:
            break
    #Allow SSM_DRAIN_TIME seconds for SSM to complete commands after a "Success" status has processed
    if status == 'Success':
        message = "Successfully sent SSM Run Command to instance " + instance_id

        print("Waiting " + str(SSM_DRAIN_TIME) + " Seconds for SSM to complete uploads.")
        time.sleep(SSM_DRAIN_TIME)
    elif status == 'InProgress':
        message = "SSM Run Command was queued, but failed to execute before timeout! OS level commands were _NOT_ performed."
    print(message)
instance_id = input('Enter the instanceId from the event:')

instanceid_is_valid(instance_id)
ec2Client = boto3.client('ec2', region_name = 'us-east-1')
ssmClient = boto3.client('ssm', region_name = 'us-east-1')


metadata(ec2Client, instance_id)
set_termination_protection(ec2Client, instance_id)

if is_instance_managed_by_SSM(ssmClient, instance_id):
    print('Instance is managed by SSM, executing commands.')
    #remove and attach SSM permissive role for information capture.
    remove_EC2_IAM_role(ec2Client, instance_id)
    attach_EC2_SSM_execution_IAM_role(ec2Client, instance_id)
    if ami_platform(ec2Client, amiId) == 'Linux/UNIX':
        ssm_send_commands(ssmClient, instance_id, OSLevelCommands=OSLevelCommandslinux, documentname=DocumentNameLinux)
    else:
        ssm_send_commands(ssmClient, instance_id, OSLevelCommands=OSLevelCommandsWindows, documentname=DocumentNameWindows)
    #After completion, remove all IAM roles.
    remove_EC2_IAM_role(ec2Client, instance_id)
else:
    print('Instance is not managed by SSM, unable to execute commands remotely')
    remove_EC2_IAM_role(ec2Client, instance_id)

def check(email):  
  
    # pass the regular expression 
    # and the string in search() method 
    if(re.search(regex,email)):  
        return  
          
    else:  
        print("Invalid Email address provided.") 
        exit() 

# Default Emailbox account
fromEmailID = None

# Validate default mail account
def validateMailbox(defaultMailAccount):
	import win32com.client
	global fromEmailID
	o = win32com.client.Dispatch("Outlook.Application")
	for account in o.Session.Accounts:
		if re.search(account.SmtpAddress, defaultMailAccount, re.IGNORECASE):
			fromEmailID = account
			break
	if fromEmailID:
		return
	else:
		print("Local mailbox account not found, please try with valid one")
		exit()
        
def send_email(subject,text):
    import win32com.client
    # s = win32com.client.Dispatch("Mapi.Session")
    o = win32com.client.Dispatch("Outlook.Application")
    # s.Logon("Outlook2003")
    Msg = o.CreateItem(0)
    Msg._oleobj_.Invoke(*(64209, 0, 8, 0, fromEmailID))
    Msg.To = emailid
    Msg.Subject = subject
    Msg.Body = text
    Msg.Send()
	
	
def get_instance_name(thisInstanceID):
ec2 = boto3.resource(‘ec2’)
ec2instance = ec2.Instance(thisInstanceID)
instancename = ”
for tags in ec2instance.tags:
if tags[“Key”] == ‘Name’:
instancename = tags[“Value”]
print(instancename)
