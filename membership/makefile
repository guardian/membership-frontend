npm-install:
	npm install

bower-install:
	cd ./app/assets/stylesheets/; $(CURDIR)/node_modules/.bin/bower install; cd -;

install: npm-install bower-install

run-tests:
	./node_modules/karma/bin/karma start