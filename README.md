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


SSH Key Setup for Github

### SHA1 is not support by Github, so generate the key with the below steps

Create file
```ssh-keygen -t ecdsa -b 256 -m PEM``

- Have pass phrase for the generated key file, Update the newly generated key in Github ssh section

- Update the Pass phrase in the Jolt-demo properties

- Update SSH config as follows pointing to the new key

```
Host github.com
  Hostname github.com
  AddKeysToAgent yes
  UseKeychain yes
  IdentityFile ~/.ssh/id_fs_git
```

- Update the knownhosts file
```ssh-keyscan -H -t ecdsa github.com >> known_hosts``

