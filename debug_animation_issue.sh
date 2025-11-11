#!/bin/bash

echo "🔍 Debugging Animation Issue..."
echo ""

# Clear logcat
adb logcat -c

echo "📱 Starting app..."
adb shell am start -n com.example.demoshemij/.MainActivity

echo ""
echo "⏳ Waiting 3 seconds..."
sleep 3

echo ""
echo "=== AnimationController Logs ==="
adb logcat -d | grep "AnimationController" | tail -20

echo ""
echo "=== JsonAnimatedSprite Logs ==="
adb logcat -d | grep "JsonAnimatedSprite" | tail -20

echo ""
echo "=== AnimationMapper Logs ==="
adb logcat -d | grep "AnimationMapper" | tail -10

echo ""
echo "=== JsonLoader Logs ==="
adb logcat -d | grep "JsonLoader" | tail -10

echo ""
echo "=== ImageLoader Logs ==="
adb logcat -d | grep "ImageLoader" | tail -10

echo ""
echo "=== Errors ==="
adb logcat -d | grep -E "ERROR|Exception" | grep -E "Animation|Json|Image" | tail -10

echo ""
echo "✅ Done! Check logs above."
