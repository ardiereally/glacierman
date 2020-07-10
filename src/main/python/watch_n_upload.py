import sys
import os
import time

if len(sys.argv) < 2:
    print("Need vault name to be provided")
    sys.exit(1)

while True:
    time.sleep(30)
    all_archives = [f for f in os.listdir('.') if (not f.startswith('done') and (f.endswith('tar.gz') or f.endswith('.zip')))]
    complete_archives = [f for f in all_archives if not os.path.exists(f'{f}.part')]
    print(f"Found {len(complete_archives)} uploadable archives")
    if not complete_archives:
        print('Nothing to upload. Will check later')
        continue
    for archive in complete_archives:
        print(f'Uploading {archive}')
        retval = os.system(f'java -jar -Dvault={sys.argv[1]} -Darchive="{archive}" glupload.jar')
        if retval != 0:
            print(f'[!!!!!] Upload failed for {archive}')
            continue
        print(f'Uploaded {archive}')
        os.rename(archive, f'done_{archive}')
        print(f'Marked as done {archive}')