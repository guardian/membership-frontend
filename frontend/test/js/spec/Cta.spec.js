define([
    '$',
    'ajax',
    'src/utils/user',
    'src/modules/events/Cta'
], function ($, ajax, user, Cta) {

    ajax.init({page: {ajaxUrl: ''}});

    describe('Early bird tickets', function() {

        var TIER_CHANGE_URL = '/tier/change';
        var JOIN_URL = '/join';
        var UPGRADE_TEXT = 'Upgrade';
        var TEST_EVENT_URL = '/test/event/url';
        var UPGRADE_COMING_SOON = 'Upgrade coming soon';
        var canonicalTicketAvailabilityFixtureElement;
        var ticketAvailabilityFixtureElement;
        var cta;
        var $saleStartElem;
        var $saleStartPatronElem;
        var $saleStartPartnerElem;
        var $saleStartFriendElem;
        var $buyTicketsCTA;
        var $memberCTA;
        var $ticketsOnSaleNow;
        var $existingMembersSignIn;

        // PhantomJS doesn't support bind yet
        Function.prototype.bind = Function.prototype.bind || function (context) {
            var fn = this;
            return function () {
                return fn.apply(context, arguments);
            };
        };

        /**
         * scala controls the button state dependant on its status Completed, SoldOut, Cancelled, PreLive, Live
         * this helper changes a buy CTA to the live state if the sales_start date has passed
         * @param startDateTimestamp
         */
        function mimicScalaButtonStates(saleStartTimestamp) {
            var nowTimestamp = (new Date()).getTime();

            if (nowTimestamp > saleStartTimestamp) {
                $('.js-ticket-cta', ticketAvailabilityFixtureElement).replaceWith("<a class='action js-ticket-cta' href=" + TEST_EVENT_URL + ">Buy Tickets</a>");
            }

            $buyTicketsCTA = $('.js-ticket-cta', ticketAvailabilityFixtureElement);
        }

        /**
         * set the dates in the fixture. This is mimicking scala behaviour and gives us control over setting the dates
         * per test
         * @param date
         * @returns {{salesStart: *, patronStart: *, partnerStart: Date, friendStart: Date}}
         */
        function setUpBookingDates(date) {
            var datePlusOneWeek = new Date(date);
            var datePlusTwoWeeks = new Date(date);

            datePlusOneWeek.setDate(datePlusOneWeek.getDate() + 7);
            datePlusTwoWeeks.setDate(datePlusTwoWeeks.getDate() + 14);

            $saleStartElem.attr('datetime', date.toISOString());
            $saleStartElem.text(formatDate(date));
            $saleStartPatronElem.attr('datetime', date.toISOString());
            $saleStartPatronElem.text(formatDate(date));
            $saleStartPartnerElem.attr('datetime', datePlusOneWeek.toISOString());
            $saleStartPartnerElem.text(formatDate(datePlusOneWeek));
            $saleStartFriendElem.attr('datetime', datePlusTwoWeeks.toISOString());
            $saleStartFriendElem.text(formatDate(datePlusTwoWeeks));

            return {
                salesStart: date,
                patronStart: date,
                partnerStart: datePlusOneWeek,
                friendStart: datePlusTwoWeeks
            }
        }

        /**
         * spy for user.loggedIn method
         * @param isLoggedIn
         */
        function setUserLoggedInStatus(isLoggedIn) {
            spyOn(user, 'isLoggedIn').and.callFake(function () {
                return isLoggedIn;
            });
        }

        /**
         * spy for user.getMemberDetail method
         * @param tier
         */
        function setUserTier(tier) {
            user.getMemberDetail.and.callFake(function (callback) {
                callback({
                    userId: '10000004',
                    tier: tier,
                    joinDate: '2014-07-29T15:43:42.000Z',
                    optIn: false
                });
            });
        }

        /**
         * format date to membership pretty date format
         * @param date
         * @returns {string}
         */
        function formatDate(date) {
            var months = [
                'January',
                'February',
                'March',
                'April',
                'May',
                'June',
                'July',
                'August',
                'September',
                'October',
                'November',
                'December'
            ];

            return date.getDate() + ' ' + months[date.getMonth()] + ' ' + date.getFullYear();
        }

        /**
         * setup the fixture
         * init the cta class
         * run standard validations
         * @param salesStart
         * @param tier
         * @param isLoggedIn
         */
        function fixtureSetup(salesStart, tier, isLoggedIn) {
            var bookingDates = setUpBookingDates(salesStart);

            mimicScalaButtonStates(bookingDates.salesStart);
            setUserLoggedInStatus(isLoggedIn);
            setUserTier(tier);

            cta.init();

            expect($saleStartPatronElem.text()).toEqual(formatDate(bookingDates.patronStart));
            expect($saleStartPartnerElem.text()).toEqual(formatDate(bookingDates.partnerStart));
            expect($saleStartFriendElem.text()).toEqual(formatDate(bookingDates.friendStart));
            expect($saleStartElem.text()).toEqual(formatDate(bookingDates.salesStart));
            expect(user.isLoggedIn).toHaveBeenCalled();
            expect(user.getMemberDetail).toHaveBeenCalled();
            expect(cta.memberTier).toEqual(tier);
            expect(cta.userIsLoggedIn).toBeTruthy();
        }

        function memBeforeEach(done) {

            //pull this in once and cache it
            if (!canonicalTicketAvailabilityFixtureElement) {
                ajax({
                    url: '/base/test/fixtures/ticketAvailabilityDetail.fixture.html',
                    method: 'get',
                    success: function (resp) {
                        canonicalTicketAvailabilityFixtureElement = $.create(resp)[0];
                        callback(canonicalTicketAvailabilityFixtureElement);
                    }
                });
            } else {
                callback(canonicalTicketAvailabilityFixtureElement);
            }

            function callback(canonicalTicketAvailabilityFixtureElement) {

                ticketAvailabilityFixtureElement = canonicalTicketAvailabilityFixtureElement.cloneNode(true);

                cta = new Cta(ticketAvailabilityFixtureElement);
                cta.elems = []; //reset component.js internal element cache

                $saleStartElem = $('.js-ticket-sale-start', ticketAvailabilityFixtureElement);
                $saleStartPatronElem = $('.js-ticket-sale-start-patron', ticketAvailabilityFixtureElement);
                $saleStartPartnerElem = $('.js-ticket-sale-start-partner', ticketAvailabilityFixtureElement);
                $saleStartFriendElem = $('.js-ticket-sale-start-friend', ticketAvailabilityFixtureElement);
                $ticketsOnSaleNow = $('.js-ticket-on-sale-now', ticketAvailabilityFixtureElement);
                $memberCTA = $('.js-member-cta', ticketAvailabilityFixtureElement);
                $existingMembersSignIn = $('.js-ticket-sale-sign-in', ticketAvailabilityFixtureElement);

                spyOn(cta, 'parseDates').and.callFake(function () {
                    cta.ticketDates = {
                        saleStart: new Date($(cta.getElem('SALE_START')).attr('datetime')),
                        saleStartFriend: new Date($(cta.getElem('SALE_START_FRIEND')).attr('datetime')),
                        saleStartPartner: new Date($(cta.getElem('SALE_START_PARTNER')).attr('datetime')),
                        saleStartPatron: new Date($(cta.getElem('SALE_START_PATRON')).attr('datetime'))
                    };
                });

                spyOn(user, 'getMemberDetail');

                done();
            }
        }

        function memAfterEach() {
            delete ticketAvailabilityFixtureElement;
            delete cta;
        }

        describe('Fixture and CTA class', function () {

            beforeEach(function (done) {
                memBeforeEach(done);
            });

            afterEach(memAfterEach);

            it(' is correctly initialised', function (done) {
                var tier = 'Friend';
                fixtureSetup(new Date(), tier, true)
                done();
            });

        });

        describe('Tickets NOT on sale', function () {

            beforeEach(function (done) {
                memBeforeEach(done);
            });

            afterEach(memAfterEach);

            var salesStartOneWeekInTheFuture = new Date();
            salesStartOneWeekInTheFuture.setDate(salesStartOneWeekInTheFuture.getDate() + 7);

            it('loggedIn Friend - "Buy Tickets" disabled, "Upgrade" button displayed and links to ' + TIER_CHANGE_URL, function (done) {

                var tier = 'Friend';
                fixtureSetup(salesStartOneWeekInTheFuture, tier, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual(UPGRADE_TEXT);
                expect($memberCTA.attr('href')).toEqual(TIER_CHANGE_URL);

                done();
            });

            it('loggedIn Partner - "Buy Tickets" disabled, "Upgrade" button disabled, no link and text says "' + UPGRADE_COMING_SOON + '"', function (done) {

                var tier = 'Partner';
                fixtureSetup(salesStartOneWeekInTheFuture, tier, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual(UPGRADE_COMING_SOON);
                expect($memberCTA.attr('href')).toBeNull();
                expect($memberCTA.hasClass('action--disabled')).toBeTruthy();

                done();
            });

            it('loggedIn Patron - "Buy Tickets" disabled, "Upgrade" button hidden', function (done) {

                var tier = 'Patron';
                fixtureSetup(salesStartOneWeekInTheFuture, tier, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.hasClass('u-h')).toBeTruthy();

                done();
            });

            it('loggedIn NOT a member - "Buy Tickets" disabled, "Become a member" button displayed and links to ' + JOIN_URL, function (done) {

                fixtureSetup(salesStartOneWeekInTheFuture, null, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual('Become a member');
                expect($memberCTA.attr('href')).toEqual(JOIN_URL);

                done();
            });

            it('loggedOut - "Buy Tickets" disabled, "Become a member" button displayed and links to ' + JOIN_URL + ', "Existing members sign in" displayed', function (done) {

                fixtureSetup(salesStartOneWeekInTheFuture, null, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual('Become a member');
                expect($memberCTA.attr('href')).toEqual(JOIN_URL);
                expect($existingMembersSignIn.parent().parent().hasClass('u-h')).toBeFalsy();

                done();
            });
        });

        describe('Tickets on sale FIRST week', function () {

            beforeEach(function (done) {
                memBeforeEach(done);
            });

            afterEach(memAfterEach);

            var saleStartedYesterday = new Date();
            saleStartedYesterday.setDate(saleStartedYesterday.getDate() - 1);

            it('loggedIn Friend - "Buy Tickets" disabled, "Upgrade" button displayed and links to ' + TIER_CHANGE_URL, function (done) {

                var tier = 'Friend';
                fixtureSetup(saleStartedYesterday, tier, true);

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual(UPGRADE_TEXT);
                expect($memberCTA.attr('href')).toEqual(TIER_CHANGE_URL);

                done();
            });

            it('loggedIn Partner - "Buy Tickets" disabled, "Upgrade" button disabled, no link and text says "' + UPGRADE_COMING_SOON + '"', function (done) {

                var tier = 'Partner';

                fixtureSetup(saleStartedYesterday, tier, true);

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual(UPGRADE_COMING_SOON);
                expect($memberCTA.attr('href')).toBeNull();
                expect($memberCTA.hasClass('action--disabled')).toBeTruthy();

                done();
            });

            it('loggedIn Patron - "Buy Tickets" enabled and links to event url, "Upgrade" button hidden, "to you now" ticket availability text displayed', function (done) {

                var tier = 'Patron';

                fixtureSetup(saleStartedYesterday, tier, true);

                expect($ticketsOnSaleNow.text()).toEqual(' to you now');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeFalsy();
                expect($buyTicketsCTA.attr('href')).toEqual(TEST_EVENT_URL);
                expect($memberCTA.hasClass('u-h')).toBeTruthy();

                done();
            });

            it('loggedIn not a member - "Buy Tickets" disabled, "Become a member" button displayed and links to ' + JOIN_URL, function (done) {

                fixtureSetup(saleStartedYesterday, null, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual('Become a member');
                expect($memberCTA.attr('href')).toEqual('/join');

                done();
            });

            it('loggedOut - "Buy Tickets" disabled, "Become a member" button displayed and links to ' + JOIN_URL + ', "Existing members sign in" displayed', function (done) {

                fixtureSetup(saleStartedYesterday, null, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual('Become a member');
                expect($memberCTA.attr('href')).toEqual('/join');
                expect($existingMembersSignIn.parent().parent().hasClass('u-h')).toBeFalsy();

                done();
            });
        });

        describe('Tickets on sale SECOND week', function () {

            beforeEach(function (done) {
                memBeforeEach(done);
            });

            afterEach(memAfterEach);

            var saleStarted8DaysAgo = new Date();
            saleStarted8DaysAgo.setDate(saleStarted8DaysAgo.getDate() - 8);

            it('loggedIn Friend - "Buy Tickets" disabled, "Upgrade" button displayed and links to ' + TIER_CHANGE_URL, function (done) {

                var tier = 'Friend';
                fixtureSetup(saleStarted8DaysAgo, tier, true);

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual(UPGRADE_TEXT);
                expect($memberCTA.attr('href')).toEqual('/tier/change');

                done();
            });

            it('loggedIn Partner - "Buy Tickets" enabled and links to event url, "Upgrade" button disabled, no link and text says "' + UPGRADE_COMING_SOON + '", "to you now" ticket availability text displayed', function (done) {

                var tier = 'Partner';
                fixtureSetup(saleStarted8DaysAgo, tier, true);

                expect($ticketsOnSaleNow.text()).toEqual(' to you now');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeFalsy();
                expect($buyTicketsCTA.attr('href')).toEqual(TEST_EVENT_URL);
                expect($memberCTA.text()).toEqual(UPGRADE_COMING_SOON);
                expect($memberCTA.attr('href')).toBeNull();
                expect($memberCTA.hasClass('action--disabled')).toBeTruthy();

                done();
            });

            it('loggedIn Patron - "Buy Tickets" enabled and links to event url, "Upgrade" button hidden, "to you now" ticket availability text displayed', function (done) {

                var tier = 'Patron';
                fixtureSetup(saleStarted8DaysAgo, tier, true);

                expect($ticketsOnSaleNow.text()).toEqual(' to you now');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeFalsy();
                expect($buyTicketsCTA.attr('href')).toEqual(TEST_EVENT_URL);
                expect($memberCTA.hasClass('u-h')).toBeTruthy();

                done();
            });

            it('loggedIn not a member - "Buy Tickets" disabled, "Become a member" button displayed and links to ' + JOIN_URL, function (done) {

                fixtureSetup(saleStarted8DaysAgo, null, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual('Become a member');
                expect($memberCTA.attr('href')).toEqual('/join');

                done();
            });

            it('loggedOut - "Buy Tickets" disabled, "Become a member" button displayed and links to ' + JOIN_URL + ', "Existing members sign in" displayed', function (done) {

                fixtureSetup(saleStarted8DaysAgo, null, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeTruthy();
                expect($buyTicketsCTA.attr('href')).toBeNull();
                expect($memberCTA.text()).toEqual('Become a member');
                expect($memberCTA.attr('href')).toEqual('/join');
                expect($existingMembersSignIn.parent().parent().hasClass('u-h')).toBeFalsy();

                done();
            });
        });

        describe('Tickets on sale GENERAL release', function () {

            beforeEach(function (done) {
                memBeforeEach(done);
            });

            afterEach(memAfterEach);

            var saleStarted15DaysAgo = new Date();
            saleStarted15DaysAgo.setDate(saleStarted15DaysAgo.getDate() - 15);

            it('loggedIn Friend - "Buy Tickets" enabled and links to event url, "Upgrade" button displayed and links to ' + TIER_CHANGE_URL, function (done) {

                var tier = 'Friend';
                fixtureSetup(saleStarted15DaysAgo, tier, true);

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeFalsy();
                expect($buyTicketsCTA.attr('href')).toEqual(TEST_EVENT_URL);
                expect($memberCTA.text()).toEqual(UPGRADE_TEXT);
                expect($memberCTA.attr('href')).toEqual('/tier/change');

                done();
            });

            it('loggedIn Partner - "Buy Tickets" enabled and links to event url, "Upgrade" button disabled, no link and text says "' + UPGRADE_COMING_SOON + '"', function (done) {

                var tier = 'Partner';
                fixtureSetup(saleStarted15DaysAgo, tier, true);

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeFalsy();
                expect($buyTicketsCTA.attr('href')).toEqual(TEST_EVENT_URL);
                expect($memberCTA.text()).toEqual(UPGRADE_COMING_SOON);
                expect($memberCTA.attr('href')).toBeNull();
                expect($memberCTA.hasClass('action--disabled')).toBeTruthy();

                done();
            });

            it('loggedIn Patron - "Buy Tickets" enabled and links to event url, "Upgrade" button hidden', function (done) {

                var tier = 'Patron';
                fixtureSetup(saleStarted15DaysAgo, tier, true);

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeFalsy();
                expect($buyTicketsCTA.attr('href')).toEqual(TEST_EVENT_URL);
                expect($memberCTA.hasClass('u-h')).toBeTruthy();

                done();
            });

            it('loggedIn not a member - "Buy Tickets" enabled and links to event url, "Upgrade" button hidden, "Become a member" button displayed and links to ' + JOIN_URL, function (done) {

                fixtureSetup(saleStarted15DaysAgo, null, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeFalsy();
                expect($buyTicketsCTA.attr('href')).toEqual(TEST_EVENT_URL);
                expect($memberCTA.text()).toEqual('Become a member');
                expect($memberCTA.attr('href')).toEqual('/join');

                done();
            });

            it('loggedOut - "Buy Tickets" enabled and links to event url, "Become a member" button displayed and links to ' + JOIN_URL + ', "Existing members sign in" displayed', function (done) {

                fixtureSetup(saleStarted15DaysAgo, null, true)

                expect($ticketsOnSaleNow.text()).toEqual('');
                expect($buyTicketsCTA.hasClass('action--disabled')).toBeFalsy();
                expect($buyTicketsCTA.attr('href')).toEqual(TEST_EVENT_URL);
                expect($memberCTA.text()).toEqual('Become a member');
                expect($memberCTA.attr('href')).toEqual('/join');
                expect($existingMembersSignIn.parent().parent().hasClass('u-h')).toBeFalsy();

                done();
            });
        });
    });
});

