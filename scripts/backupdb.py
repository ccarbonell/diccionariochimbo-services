import os
import sys
import time

#IF YOU ARE TESTING IN WINDOWS YOU'LL NEED TO CHANGE THIS PATH
MONGODUMP='mongodump'

def zeroPad(number):
  if number < 10:
    return '0'+str(number)
  return str(number)

def createBackupDir():
  '''
  Create the backup dir if it doesn't exist.

  Returns the backup folder path.
  '''
  DATE=time.gmtime()
  DATE_YEAR=str(DATE[0])
  DATE_MONTH=zeroPad(DATE[1])
  DATE_DAY=zeroPad(DATE[2])
  BACKUPS_DIR='..'+os.sep+'db_backups'
  BACKUP_FOLDER_NAME='dump_'+DATE_YEAR+'_'+DATE_MONTH+'_'+DATE_DAY
  BACKUP_PATH=BACKUPS_DIR + os.sep + BACKUP_FOLDER_NAME

  try:
    os.makedirs(BACKUP_PATH)
  except:
    pass

  return BACKUP_PATH

def dumpDB(dumpPath):
  '''Dump the current DC database to the given dumpPath'''
  os.system(MONGODUMP + ' --db dc --out ' + dumpPath)

if __name__=='__main__':
  dumpDB(createBackupDir())
