#!/bin/bash

# Script for uploading APK to OneDrive
# Required environment variables:
# - GRAPH_CLIENT_ID
# - GRAPH_CLIENT_SECRET
# - GRAPH_TENANT_ID
# - GRAPH_DRIVE_ID
# - ONEDRIVE_DIR - for APK
# - ONEDRIVE_BASE_DIR - for latest APK
# - BUILD_FILENAME
# - FILEPATH
# - FILENAME
# - LATEST_PREFIX
# - EXTENSION


access_token=$(curl -X POST "https://login.microsoftonline.com/$GRAPH_TENANT_ID/oauth2/v2.0/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=$GRAPH_CLIENT_ID" \
  -d "client_secret=$GRAPH_CLIENT_SECRET" \
  -d "scope=https://graph.microsoft.com/.default" \
  -d "grant_type=client_credentials" | jq -r '.access_token')

file_name=$BUILD_FILENAME
file_content=$(cat $FILEPATH | base64)
upload_url="https://graph.microsoft.com/v1.0/drives/$GRAPH_DRIVE_ID/items/root:$ONEDRIVE_DIR/$FILENAME:/content"
latest_apk_url="https://graph.microsoft.com/v1.0/drives/$GRAPH_DRIVE_ID/items/root:$ONEDRIVE_BASE_DIR/$LATEST_PREFIX-latest$EXTENSION:/content"

# Upload the APK to OneDrive
response=$(curl -X PUT "${upload_url}" -H "Authorization: Bearer ${access_token}" -H "Content-Type: application/octet-stream" --data-binary @${FILEPATH})
echo "Upload response: $response"
item_id=$(echo $response | jq -r '.id')
echo "item_id=${item_id}"

# Upload the APK to OneDrive latest
echo "Uploading file to OneDrive latest..."
response=$(curl -X PUT "${latest_apk_url}" -H "Authorization: Bearer ${access_token}" -H "Content-Type: application/octet-stream" --data-binary @${FILEPATH})
item_latest_id=$(echo $response | jq -r '.id')
echo "item_latest_id=${item_latest_id}"

# Create a public shareable link
link_url="https://graph.microsoft.com/v1.0/drives/$GRAPH_DRIVE_ID/items/${item_id}/createLink"
expiration_date=$(date -u -d '+6 months' '+%Y-%m-%dT%H:%M:%SZ')
link_data='{"type": "view", "scope": "anonymous", "retainInheritedPermissions": false, "expirationDateTime": "'"$expiration_date"'"}'
link_response=$(curl -X POST "${link_url}" -H "Authorization: Bearer ${access_token}" -H "Content-Type: application/json" -d "${link_data}")

link_url="https://graph.microsoft.com/v1.0/drives/$GRAPH_DRIVE_ID/items/${item_latest_id}/createLink"
link_data='{"type": "view", "scope": "anonymous", "retainInheritedPermissions": false}'
link_response_latest=$(curl -X POST "${link_url}" -H "Authorization: Bearer ${access_token}" -H "Content-Type: application/json" -d "${link_data}")

# Extract public link from the response
public_link="$(echo $link_response | jq -r '.link.webUrl')?download=1"
public_link_latest="$(echo $link_response_latest | jq -r '.link.webUrl')?download=1"
echo "ONEDRIVE_LINK=${public_link}" >> $GITHUB_ENV
echo "ONEDRIVE_LINK_LATEST=${public_link_latest}" >> $GITHUB_ENV