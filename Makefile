all: build

jni/sqlite3.c:
	rm -fr tmp
	mkdir -p tmp
	cd tmp
	wget --no-clobber http://www.sqlite.org/sqlite-autoconf-3071501.tar.gz -P tmp/
	tar -xzf tmp/sqlite-autoconf-3071501.tar.gz -C tmp/
	cp -fv tmp/sqlite-autoconf-3071501/sqlite3.[ch] ./jni/

build: jni/sqlite3.c
	ndk-build
	mv -fv libs/armeabi/keystorecmd libs/armeabi/libkeystorecmd-executable.so
	mv -fv libs/armeabi/unlockcmd libs/armeabi/libunlockcmd-executable.so
	wget --no-clobber https://s3.amazonaws.com/bugsenseplugins/bugsense3.4.jar -P libs/
	ant release

deploy: build
	adb logcat -c
	ant installr
	adb shell am start -a android.intent.action.MAIN -n ru.chunky.AutoKeystore/.Settings
	adb logcat -v time -s '*:e' 'AutoKeystore' 'keystore' || true


clean:
	ant clean
	ndk-build clean
