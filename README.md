jolt-demo
=========

Demo and Doc site for Jolt

# Demo Server

http://jolt-demo.appspot.com

# Tech

Google App Engine has a truly free tier, so building a small war so that people can play with and deploying there.

#FW Update
Check style
```mvn checkstyle:checkstyle``

Locate the checkstyle file
```cd target/checkstyle-result.xml``

Use eclipse check style plugin to fix the error at development time.

SSH Key generation to do the JGIT actions

Generate with passphrase
```
ssh-keygen -t ecdsa -b 256 -m PEM
```
- Add the ssh in the Github ssh list

Add the generated file in the ssh config
```
Host github.com
  Hostname github.com
  AddKeysToAgent yes
  UseKeychain yes
  IdentityFile ~/.ssh/id_fs_git1
```
Add the file in the know_hosts. This will be automatically added when you connect ssh with the github through terminal.
