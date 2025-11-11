#!/bin/bash

# Script để debug animation system

echo "🔍 Checking animation logs..."
echo ""
echo "=== JsonLoader Logs ==="
adb logcat -d | grep "JsonLoader" | tail -20
echo ""
echo "=== AnimationMapper Logs ==="
adb logcat -d | grep "AnimationMapper" | tail -20
echo ""
echo "=== AnimationController Logs ==="
adb logcat -d | grep "AnimationController" | tail -20
echo ""
echo "=== JsonAnimatedSprite Logs ==="
adb logcat -d | grep "JsonAnimatedSprite" | tail -20
echo ""
echo "=== All Animation Related Errors ==="
adb logcat -d | grep -E "JsonLoader|AnimationMapper|AnimationController|JsonAnimatedSprite" | grep -E "ERROR|Exception" | tail -20
