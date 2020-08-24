import os
import re
import time
from datetime import datetime

def decide_vault(archive_name):
    name, _ = archive_name.rsplit('.', 1)
    if re.match(r"^.* \(\d+\)$", name):
        return "Movies"
    if re.match(r"^.* - (Specials|Season \d+)$", name):
        return "TVShows"
    raise ValueError("Couldn't match " + name)

uploaded_archives = set()

while True:
    time.sleep(30)
    all_archives = [f for f in os.listdir('.') if ((not f.startswith('done')) and f.endswith('.zip'))]
    complete_archives = [f for f in all_archives if not os.path.exists(f'{f}.part')]
    pending_archives = [f for f in complete_archives if f not in uploaded_archives]
    print(f"Found {len(pending_archives)} uploadable archives")
    if not pending_archives:
        print(f'Nothing to upload as of {datetime.now()}. Will check later')
        continue
    for archive in pending_archives:
        vault_name = decide_vault(archive)
        print(f'Uploading {archive} to {vault_name}')
        retval = os.system(f'java -jar -Dvault={vault_name} -Darchive="{archive}" glupload.jar')
        if retval != 0:
            print(f'[!!!!!] Upload failed for {archive}')
            continue
        print(f'Uploaded {archive}')
        try:
            os.rename(archive, f'done_{archive}')
        except OSError:
            print(f'Could not rename. Restarting script might re-upload {archive}')
        uploaded_archives.add(archive)
        print(f'Marked as done {archive}')
        print('====================================================================')
