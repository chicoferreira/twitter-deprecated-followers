# Twitter Deprecated Followers

## Description

A simple java jar to get the list of inactive followers from Twitter using Twitter SDK for Java.

## Requirements

- Java 17 (Should work in earlier versions)

## Usage

- Apply for Twitter Dev and create a new application on Twitter Dev
  Portal (https://developer.twitter.com/en/portal/dashboard).
- Download the jar file from releases page.
- Create a file `credentials.txt` in the same folder as the jar and paste the Bearer token from the Twitter App in it.
- Run ```java -jar twitter-deprecated-followers.jar <user target without @>```

## Compile from source

### Requirements

- Git
- Java SDK 17
- Gradle

### Building
```bash
git clone https://github.com/chicoferreira/twitter-deprecated-followers
cd twitter-deprecated-followers
# Linux or MacOS
./gradlew build
# Windows
./gradlew.bat build
```
You will find the built jar file in the `build/libs` folder.
