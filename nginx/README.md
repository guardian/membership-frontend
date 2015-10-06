# Membership NGINX

To run standalone you can use the default nginx installation as follows:

Install nginx:

- Linux: `sudo apt-get install nginx`
- Mac OSX: `brew install nginx`

Make sure you have a `sites-enabled/` folder under your nginx home. This varies depending on your setup but should be under:

- Linux: `/etc/nginx/sites-enabled`
- Mac OSX: `/usr/local/etc/nginx/sites-enabled`

Make sure your `nginx.conf` (found in your nginx home) contains the following line in the `http{...}` block:

```
include sites-enabled/*
```

If you want an automated setup run:

```
./nginx/setup.sh
```

Or follow the steps in that file.

### Update your hosts file

Add the following to your hosts file in `/etc/hosts`:

```
127.0.0.1   mem.thegulocal.com
127.0.0.1   profile.thegulocal.com
```
