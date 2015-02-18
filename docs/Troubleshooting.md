# Troubleshooting

## NPM Hangs or doesn't download all dependencies

If you run `./setup.sh` and npm doesn't install all of its dependencies it may help to do the following
1. `cd ./frontend`
2. Remove node folder `rm -rf node_modules`
3. Run `npm cache clean`
4. Run `./setup.sh` to install project-specific client-side dependencies.

## NPM "EACCES"

If you get errors like this on `npm install`
```
npm WARN locking Error: EACCES, open '/Users/jduffell/.npm/_locks/karma-requirejs-4becac899d6c8f35.lock'
```

Sometimes when you install npm, it ends up owned by root (but in your home
directory).

Check that you own your own .npm directory `ls -ld ~/.npm`

If it is owned by root, then take ownership of it
`sudo chown -R username:username ~/.npm`


## File handles - "Too many files open"

You may run into a "too many files open" error during
compilation or reloading. You can find out how many file handles you are
allowed per process by running `ulimit -n`. This can be quite low, e.g. 1024 on linux or 256 on Mac

### Linux

To increase the limit do the following (instructions from Ubuntu 12.10)...

In the file `/etc/security/limits.conf` add the following two lines
```
*  soft  nofile 20000
*  hard  nofile 65000
```

And in the file `/etc/pam.d/common-session` add the following line.
```
session required pam_limits.so
```

Restart the machine.

For more info see http://www.cyberciti.biz/faq/linux-increase-the-maximum-number-of-open-files/

### Mac

* Edit your `~/.bash-profile` file
* add the following line: `ulimit -n 1024`
* save and close the file
* back at the command prompt enter: `source .bash_profile` and hit return.

Now you should be able to compile and run. Yay.
