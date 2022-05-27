export function init() {
    const _linkedin_partner_id = '2929546';

    window._linkedin_data_partner_ids = window._linkedin_data_partner_ids || [];
    window._linkedin_data_partner_ids.push(_linkedin_partner_id);

    if (!window.lintrk) {
        window.lintrk = (a, b) => window.lintrk.q.push([a, b]);
        window.lintrk.q = [];
    }
    var s = document.getElementsByTagName('script')[0];

    var b = document.createElement('script');

    b.type = 'text/javascript';
    b.async = true;
    b.src = 'https://snap.licdn.com/li.lms-analytics/insight.min.js';

    s.parentNode.insertBefore(b, s);
}

export const vendorName = 'LinkedIn Pixel';
export const cmpVendorId = '5ed0eb688a76503f1016578f';
