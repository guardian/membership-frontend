/**
 * Rules for the CTA
 * - If event is not on sale to anyone return 'unavailable' status object
 * - If event is within priority booking mode and user is not logged-in return 'join' status object
 * - If event is within priority booking mode, and user is logged-in user with a tier sale start date in the future return 'upgrade' status object
 * - If event is within priority booking mode, and user is logged-in user with a tier sale start date in the past return empty status object
 * - If event is on sale to everyone, and user is logged-in with or without a tier return empty status object
 */
define(['src/modules/events/cta'], function (cta) {

    describe('Cta tests', function () {

        describe('getCtaStatus', function () {

            it('Event is not on sale to anyone' , function () {
                var startDate = new Date('1 Jan 2198');
                var endDate = new Date('1 Jan 2199');
                var patronSaleStart = new Date(startDate.getTime());

                it('with no user', function () {
                    expect(cta.getCtaStatus(startDate, endDate, null).id).toEqual('unavailable');
                });

                it('with patron tier', function () {
                    expect(cta.getCtaStatus(startDate, endDate, patronSaleStart).id).toEqual('unavailable');
                });
            });

            describe('Event is within priority booking mode', function () {
                var startDate = new Date('1 Jan 2015');
                var endDate = new Date('1 Jan 2199');
                var friendSaleStart = new Date(endDate.getTime());
                var patronSaleStart = new Date(startDate.getTime());

                it('with no user', function () {
                    expect(cta.getCtaStatus(startDate, endDate, null).id).toEqual('join');
                });

                it('with friend tier', function () {
                    expect(cta.getCtaStatus(startDate, endDate, friendSaleStart).id).toEqual('upgrade');
                });

                it('with patron tier', function () {
                    expect(cta.getCtaStatus(startDate, endDate, patronSaleStart).id).toBeUndefined();
                });
            });

            describe('Event is on sale to everyone', function () {
                var startDate = new Date('1 Jan 2015');
                var endDate = new Date('1 Feb 2015');
                var friendSaleStart = new Date(endDate.getTime());
                var patronSaleStart = new Date(startDate.getTime());

                it('with no user', function () {
                    expect(cta.getCtaStatus(startDate, endDate, null).id).toBeUndefined();
                });

                it('with friend tier', function () {
                    expect(cta.getCtaStatus(startDate, endDate, friendSaleStart).id).toBeUndefined();
                });

                it('with patron tier', function () {
                    expect(cta.getCtaStatus(startDate, endDate, patronSaleStart).id).toBeUndefined();
                });
            });
        });
    });
});
