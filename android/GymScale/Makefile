rn-start:
	npx react-native start

run-android:
	npx react-native run-android

release-android:
	npx react-native run-android --variant=release

repl:
	clj -M -m krell.main -h localhost -co build.edn -c -r

compile:
	clj -M -m krell.main -h localhost -co build.edn -c

reverse:
	adb -s GCC0X8081166034J reverse tcp:8081 tcp:8081
	adb -s GCC0X8081166034J reverse tcp:5001 tcp:5001
