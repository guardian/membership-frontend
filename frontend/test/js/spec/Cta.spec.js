define(['src/modules/events/cta'], function (cta) {

    describe('newStatus', function () {

        it('be unavailable if off-sale to everyone' , function () {
            var preSaleStart = new Date('1 Jan 2199');
            var preSaleEnd = new Date('1 Jan 2199');

            expect(cta.newStatus(preSaleStart, preSaleEnd)).toEqual('unavailable');
            expect(cta.newStatus(preSaleStart, preSaleEnd), new Date('1 Feb 2015')).toEqual('unavailable');
            expect(cta.newStatus(preSaleStart, preSaleEnd), new Date('31 Dec 2198')).toEqual('unavailable');
            expect(cta.newStatus(preSaleStart, preSaleEnd), false).toEqual('unavailable');
        });

        describe('Event in pre-sale mode', function () {
            it('should prompt to become a member if pre-sale but logged-out' , function () {
                var preSaleStart = new Date('1 Jan 2015');
                var preSaleEnd = new Date('1 Jan 2199');

                expect(cta.newStatus(preSaleStart, preSaleEnd)).toEqual('join');
            });
            it('should prompt to upgrade if the users tier cannot buy tickets yet' , function () {

                var preSaleStart = new Date('1 Jan 2015');
                var preSaleEnd = new Date('1 Jan 2199');
                var tierDate = new Date('31 Dec 2198');

                expect(cta.newStatus(preSaleStart, preSaleEnd, tierDate)).toEqual('upgrade');
            });
            it('should be bookable if the users tier can buy tickets' , function () {

                var preSaleStart = new Date('1 Jan 2015');
                var preSaleEnd = new Date('1 Jan 2199');
                var tierDate = new Date('1 Feb 2015');

                /**
                 * Should return status as bookable is the default action,
                 * so don't do anything.
                 */
                expect(cta.newStatus(preSaleStart, preSaleEnd, tierDate)).toEqual(false);
            });
        });

    });

});

