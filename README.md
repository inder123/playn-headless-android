playn-headless-android
=========

A headless version of PlayN for the Android platform

Release Process:

1. mvn release:prepare
2. mvn -s ~/.m2/settings.xml release:perform
3. Login to Nexus Repository Manager at https://oss.sonatype.org/index.html and close the staging repository for playn-headless-android
If you run into an error regarding missing signatures, you need to manually upload the artifacts using mvn gpgp:sign-and-deploy-file for the binary, source and javadoc jars.
4. After the close is successful:
Download and sanity check all downloads. Do not skip this step! Once you release the staging repository, there is no going back. It will get synced with maven central and you will not be able to update or delete anything. Your only recourse will be to release a new version and hope that no one uses the old one.
5. Release the staging repository for playn-headless-android. It will now get synced to Maven central with-in the next hour. For issues consult Sonatype Guide (https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide#SonatypeOSSMavenRepositoryUsageGuide-8.ReleaseIt).
