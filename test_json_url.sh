#!/bin/bash

echo "Testing JSON URL..."
echo ""

# Test JSON URL
JSON_URL="https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/data.json"

echo "1. Testing JSON URL: $JSON_URL"
curl -I "$JSON_URL" 2>&1 | head -5

echo ""
echo "2. Downloading JSON (first 100 lines)..."
curl -s "$JSON_URL" | head -100

echo ""
echo "3. Testing image URL..."
IMAGE_URL="https://wallpaperhd.nyc3.cdn.digitaloceanspaces.com/Shimeji/vampire/climb1.png"
curl -I "$IMAGE_URL" 2>&1 | head -5

echo ""
echo "Done!"
