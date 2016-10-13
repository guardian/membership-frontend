# Membership NGINX

To run standalone you can use the default nginx installation as follows:

Install nginx:

- Linux: `sudo apt-get install nginx`
- Mac OSX: `brew install nginx`

The nginx home directory will contain the nginx configuration and will need to be modified for membership. Depending on your system it should be under:

- Linux: `/etc/nginx/`
- Mac OSX: `/usr/local/etc/nginx/`

Make sure your `nginx.conf` (found in your nginx home) contains the following line in the `http{...}` block:

```
include sites-enabled/*;
```

If you want an automated setup run (from the project root):

```
./nginx/setup.sh
```

Or follow the steps in that file. This will create a `site-enabled directory` in you nginx home directory. Make sure you get your AWS credentials set up before you run this.

### Update your hosts file

Add the following to your hosts file in `/etc/hosts`:

```
127.0.0.1   mem.thegulocal.com
127.0.0.1   profile.thegulocal.com
```

### Start NGINX
`./nginx/setup.sh` will start nginx as its final step. To manually start it run `sudo nginx` or `sudo systemctl nginx start` depending on your system.

### NGINX error messages

nginx has some unhelpful error messages. Here are some translations:

##### When stopping/reloading nginx
```
nginx: [error] open() "/usr/local/var/run/nginx.pid" failed (2: No such file or directory)
```

This means nginx is **not running**. And `nginx -s reload` will not automatically start nginx if it's not running.

##### When starting nginx
```
nginx: [emerg] bind() to 0.0.0.0:8080 failed (48: Address already in use)
```

This means nginx is **already running**.
