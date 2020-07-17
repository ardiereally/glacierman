import json
from typing import Dict

SIZE_DIFF_THRESHOLD = 256 * 1024  # kb

def load_local(filename) -> Dict[str, int]:
    archives = dict()
    with open(filename) as f:
        for line in f:
            size_str, name = line.split(maxsplit=1)
            size_bytes = int(size_str) * 1024
            name = name.replace('./', '').replace('$', '').rstrip()
            archives[name] = size_bytes
    return archives

def load_remote(filename) -> Dict[str, int]:
    archives = dict()
    with open(filename) as f:
        inventory = json.load(f)
    for archive in inventory['ArchiveList']:
        description = archive['ArchiveDescription']
        size_bytes = archive['Size']
        name = description.replace('Glacier backup of ', '').replace('.zip', '')
        archives[name] = size_bytes
    return archives

def compare_archives(local: Dict[str, int], remote: Dict[str, int]):
    for name, local_size in local.items():
        if name not in remote:
            print(f"[ERROR] {name} is missing from remote")
            continue
        remote_size = remote[name]
        if abs(local_size - remote_size) > SIZE_DIFF_THRESHOLD:
            print(f"[WARN] {name} size difference is greater than {SIZE_DIFF_THRESHOLD}")

if __name__ == '__main__':
    local_movies = load_local('movies-checksum.txt')
    remote_movies = load_remote('Movies_inventory.json')
    compare_archives(local_movies, remote_movies)
