# Build Documentation

## Prerequisites

Install Java 11:

```bash
sudo apt update
sudo apt install -y openjdk-11-jdk
```
If another Java version is already installed, list installed Java versions:
```bash
ls /usr/lib/jvm
```
Select Java 11 as the default:
```bash
sudo update-alternatives --config java
sudo update-alternatives --config javac
```
Verify that Java 11 is active:
```bash
java -version
javac -version
```
⚠️ This project is not compatible with Java 17. Make sure Java 11 is selected before running sbt.

## Install sbt

Add the sbt repository and key:

```bash
echo "deb https://repo.scala-sbt.org/scalasbt/debian all main" | sudo tee /etc/apt/sources.list.d/sbt.list
echo "deb https://repo.scala-sbt.org/scalasbt/debian /" | sudo tee /etc/apt/sources.list.d/sbt_old.list
curl -sL "https://keyserver.ubuntu.com/pks/lookup?op=get&search=0x2EE0EA64E40A89B84B2DF73499E82A75642AC823" | sudo tee /etc/apt/trusted.gpg.d/sbt.asc
```

Install sbt:

```bash
sudo apt-get update
sudo apt-get install sbt
```

Verify installation:

```bash
sbt --version
```

## Build

Compile the project:

```bash
sbt compile
```
