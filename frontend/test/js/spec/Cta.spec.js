define([
    '$',
    'ajax',
    'src/utils/user',
    'src/modules/events/Cta'
], function ($, ajax, user, Cta) {

    ajax.init({page: {ajaxUrl: ''}});

    describe('Early bird tickets', function() {

        var BUY_TICKETS = 'Book Tickets';
        var COMING_SOON = 'Coming Soon';
        var BECOME_A_MEMBER = 'Become a member';
        var UPGRADE_MEMBERSHIP = 'Upgrade membership';
        var TICKETS_AVAILABLE_SOON = 'Tickets available soon';
        var cta;

        // PhantomJS doesn't support bind yet
        Function.prototype.bind = Function.prototype.bind || function (context) {
            var fn = this;
            return function () {
                return fn.apply(context, arguments);
            };
        };

        /**
         * set the dates up for use in the class. This is mimicking scala behaviour and gives us control over setting the dates
         * per test
         * @param date
         * @returns {{salesStart: *, patronStart: *, partnerStart: Date, friendStart: Date}}
         */
        function setUpBookingDates(date) {
            var datePlusOneWeek = new Date(date);
            var datePlusTwoWeeks = new Date(date);

            datePlusOneWeek.setDate(datePlusOneWeek.getDate() + 7);
            datePlusTwoWeeks.setDate(datePlusTwoWeeks.getDate() + 14);

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
         * setup the fixture
         * init the cta class
         * run standard validations
         * @param salesStart
         * @param tier
         * @param isLoggedIn
         */
        function ctaClassSetup(salesStart, tier, isLoggedIn) {
            var bookingDates = setUpBookingDates(salesStart);

            setUserLoggedInStatus(isLoggedIn);
            setUserTier(tier);

            spyOn(cta, 'parseDates').and.callFake(function () {
                cta.ticketDates = {
                    saleStart: bookingDates.salesStart,
                    saleStartFriend: bookingDates.friendStart,
                    saleStartPartner: bookingDates.partnerStart,
                    saleStartPatron: bookingDates.patronStart
                };
            });

            spyOn(cta, 'disableBuyTicketsCtaButton').and.callThrough();
            spyOn(cta, 'upgradeMemberCtaButton').and.callThrough();
            spyOn(cta, 'removeMemberCtaButton').and.callThrough();

            cta.init();

            expect(user.isLoggedIn).toHaveBeenCalled();
            expect(user.getMemberDetail).toHaveBeenCalled();
            expect(cta.memberTier).toEqual(tier);
            expect(cta.userIsLoggedIn).toBeTruthy();
        }

        function memBeforeEach(done) {
            cta = new Cta($.create('<div class="event__tickets"><div class="js-ticket-cta"></div><div class="js-member-cta"></div></div>'));
            cta.elems = []; //reset component.js internal element cache

            spyOn(user, 'getMemberDetail');

            done();
        }

        describe('Fixture and CTA class', function () {

            beforeEach(function (done) {
                memBeforeEach(done);
            });


            it(' is correctly initialised', function (done) {
                var tier = 'Friend';
                ctaClassSetup(new Date(), tier, true)
                done();
            });

        });

        describe('Tickets NOT on sale', function () {

            beforeEach(function (done) {
                memBeforeEach(done);
            });


            var salesStartOneWeekInTheFuture = new Date();
            salesStartOneWeekInTheFuture.setDate(salesStartOneWeekInTheFuture.getDate() + 7);

            it('loggedIn Friend - "' + COMING_SOON + '" button disabled and "' + BECOME_A_MEMBER + '" button NOT displayed' , function (done) {
                ctaClassSetup(salesStartOneWeekInTheFuture, 'Friend', true)
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).toHaveBeenCalled();
                done();
            });

            it('loggedIn Partner - "' + COMING_SOON + '" button disabled and "' + BECOME_A_MEMBER + '" button NOT displayed', function (done) {
                ctaClassSetup(salesStartOneWeekInTheFuture, 'Partner', true)
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).toHaveBeenCalled();
                done();
            });

            it('loggedIn Patron - "' + COMING_SOON + '" button disabled and "' + BECOME_A_MEMBER + '" button NOT displayed', function (done) {
                ctaClassSetup(salesStartOneWeekInTheFuture, 'Patron', true)
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).toHaveBeenCalled();
                done();
            });

            it('loggedIn NOT a member - "' + COMING_SOON + '" button disabled and "' + BECOME_A_MEMBER + '" button displayed', function (done) {
                ctaClassSetup(salesStartOneWeekInTheFuture, null, true)
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });

            it('loggedOut - "' + COMING_SOON + '" button disabled and "' + BECOME_A_MEMBER + '" button displayed', function (done) {
                ctaClassSetup(salesStartOneWeekInTheFuture, null, true)
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });
        });

        describe('Tickets on sale FIRST week', function () {

            beforeEach(function (done) {
                memBeforeEach(done);
            });

            var saleStartedYesterday = new Date();
            saleStartedYesterday.setDate(saleStartedYesterday.getDate() - 1);

            it('loggedIn Friend - "' + BUY_TICKETS + '" button disabled and "' + UPGRADE_MEMBERSHIP + '" button displayed', function (done) {
                ctaClassSetup(saleStartedYesterday, 'Friend', true);
                expect(cta.disableBuyTicketsCtaButton).toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });

            it('loggedIn Partner - "' + BUY_TICKETS + '" button disabled and "' + TICKETS_AVAILABLE_SOON + '" button displayed', function (done) {
                ctaClassSetup(saleStartedYesterday, 'Partner', true);
                expect(cta.disableBuyTicketsCtaButton).toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });

            it('loggedIn Patron - "' + BUY_TICKETS + '" enabled and memberCTA button removed', function (done) {

                ctaClassSetup(saleStartedYesterday, 'Patron', true);

                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).toHaveBeenCalled();

                done();
            });

            it('loggedIn not a member - "' + BUY_TICKETS + '" disabled and "' + BECOME_A_MEMBER + '" button displayed', function (done) {

                ctaClassSetup(saleStartedYesterday, null, true)

                expect(cta.disableBuyTicketsCtaButton).toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();

                done();
            });

            it('loggedOut - "' + BUY_TICKETS + '" disabled and "' + BECOME_A_MEMBER + '" button displayed', function (done) {

                ctaClassSetup(saleStartedYesterday, null, true)

                expect(cta.disableBuyTicketsCtaButton).toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();

                done();
            });
        });

        describe('Tickets on sale SECOND week', function () {

            beforeEach(function (done) {
                memBeforeEach(done);
            });

            var saleStarted8DaysAgo = new Date();
            saleStarted8DaysAgo.setDate(saleStarted8DaysAgo.getDate() - 8);

            it('loggedIn Friend - "' + BUY_TICKETS + '" disabled  and "' + UPGRADE_MEMBERSHIP + '" button displayed', function (done) {
                ctaClassSetup(saleStarted8DaysAgo, 'Friend', true);
                expect(cta.disableBuyTicketsCtaButton).toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).toHaveBeenCalled();
                done();
            });

            it('loggedIn Partner - "' + BUY_TICKETS + '" enabled and memberCTA button removed', function (done) {
                ctaClassSetup(saleStarted8DaysAgo, 'Partner', true);
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).toHaveBeenCalled();
                done();
            });

            it('loggedIn Patron - "' + BUY_TICKETS + '" enabled and memberCTA button removed', function (done) {
                ctaClassSetup(saleStarted8DaysAgo, 'Patron', true);
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).toHaveBeenCalled();
                done();
            });

            it('loggedIn not a member - "' + BUY_TICKETS + '" disabled and "' + BECOME_A_MEMBER + '" button displayed', function (done) {
                ctaClassSetup(saleStarted8DaysAgo, null, true)
                expect(cta.disableBuyTicketsCtaButton).toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });

            it('loggedOut - "' + BUY_TICKETS + '" disabled and "' + BECOME_A_MEMBER + '" button displayed', function (done) {
                ctaClassSetup(saleStarted8DaysAgo, null, true)
                expect(cta.disableBuyTicketsCtaButton).toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });
        });

        describe('Tickets on sale FRIEND release', function () {

            beforeEach(function (done) {
                memBeforeEach(done);
            });

            var saleStarted15DaysAgo = new Date();
            saleStarted15DaysAgo.setDate(saleStarted15DaysAgo.getDate() - 15);

            it('loggedIn Friend - "' + BUY_TICKETS + '" enabled and memberCTA button removed', function (done) {
                ctaClassSetup(saleStarted15DaysAgo, 'Friend', true);
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });

            it('loggedIn Partner - "' + BUY_TICKETS + '" enabled and memberCTA button removed', function (done) {
                ctaClassSetup(saleStarted15DaysAgo, 'Partner', true);
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });

            it('loggedIn Patron - "' + BUY_TICKETS + '" enabled and memberCTA button removed', function (done) {
                ctaClassSetup(saleStarted15DaysAgo, 'Patron', true);
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });

            it('loggedIn not a member - "' + BUY_TICKETS + '" enabled and memberCTA button removed', function (done) {
                ctaClassSetup(saleStarted15DaysAgo, null, true)
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });

            it('loggedOut - "' + BUY_TICKETS + '" enabled and memberCTA button removed', function (done) {
                ctaClassSetup(saleStarted15DaysAgo, null, true)
                expect(cta.disableBuyTicketsCtaButton).not.toHaveBeenCalled();
                expect(cta.upgradeMemberCtaButton).not.toHaveBeenCalled();
                expect(cta.removeMemberCtaButton).not.toHaveBeenCalled();
                done();
            });
        });
    });
});

