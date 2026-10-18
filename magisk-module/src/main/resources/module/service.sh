#!/system/bin/sh

# DSU Sideloader Shared Folder
# The host app writes the relative primary-storage path here. In a DSU guest,
# /mnt/pass_through exposes the host's /data/media tree on supported builds.
CONFIG=/mnt/pass_through/0/emulated/0/DSU-Sideloader/.shared_folder
SOURCE_ROOT=/mnt/pass_through/0/emulated/0
TARGET_ROOT=/storage/emulated/0

# Wait for Android storage/pass-through mounts to become available.
i=0
while [ ! -r "$CONFIG" ] && [ "$i" -lt 30 ]; do
    sleep 1
    i=$((i + 1))
done

[ -r "$CONFIG" ] || exit 0
RELATIVE_PATH=$(cat "$CONFIG")

# Do not allow traversal or absolute paths from the config file.
case "$RELATIVE_PATH" in
    ""|/*|*..*|*"//"*) exit 0 ;;
esac

SOURCE="$SOURCE_ROOT/$RELATIVE_PATH"
TARGET="$TARGET_ROOT/$RELATIVE_PATH"
[ -d "$SOURCE" ] || exit 0

mkdir -p "$TARGET"
[ -d "$TARGET" ] || exit 0

# Avoid hiding an already mounted target and avoid binding a path onto itself.
mountpoint -q "$TARGET" 2>/dev/null && exit 0
[ "$(readlink -f "$SOURCE" 2>/dev/null)" = "$(readlink -f "$TARGET" 2>/dev/null)" ] && exit 0

mount --bind "$SOURCE" "$TARGET" 2>/dev/null || exit 0
# Keep the shared directory writable when the underlying mount permits it.
mount -o remount,bind,rw "$TARGET" 2>/dev/null || true
