```
                            __                            __
 /'\_/`\                   /\ \                          /\ \      __
/\      \     __    ___ ___\ \ \____     __   _ __   ____\ \ \___ /\_\  _____
\ \ \__\ \  /'__`\/' __` __`\ \ '__`\  /'__`\/\`'__\/',__\\ \  _ `\/\ \/\ '__`\
 \ \ \_/\ \/\  __//\ \/\ \/\ \ \ \L\ \/\  __/\ \ \//\__, `\\ \ \ \ \ \ \ \ \L\ \
  \ \_\\ \_\ \____\ \_\ \_\ \_\ \_,__/\ \____\\ \_\\/\____/ \ \_\ \_\ \_\ \ ,__/
   \/_/ \/_/\/____/\/_/\/_/\/_/\/___/  \/____/ \/_/ \/___/   \/_/\/_/\/_/\ \ \/
                                                                          \ \_\
                                                                           \/_/
```

# Membership App

## Setup

1. Go to project root
1. ./setup.sh
1. Add the following to your `/etc/hosts`

   ```
   127.0.0.1   mem.thegulocal.com
   127.0.0.1   memapi.thegulocal.com
   ```

1. ./nginx/setup.sh

## Run
The API and frontend normally run on ports 9100 and 9101 respectively.
You can run the following commands to start them (separate console windows)

```
./start-frontend.sh
./start-api.sh
```

## To run frontend tests

+ $ karma start

# Grunt Tasks

## Watch and compile front-end files
+ $ grunt watch

## Compile front-end files
+ $ grunt compile
