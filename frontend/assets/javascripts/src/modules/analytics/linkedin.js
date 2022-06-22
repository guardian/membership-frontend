export function init() {
    const _linkedin_partner_id = '4461137';

    window._linkedin_data_partner_ids = window._linkedin_data_partner_ids || [];
    window._linkedin_data_partner_ids.push(_linkedin_partner_id);

    if (!window.lintrk) {
        window.lintrk = (a, b) => window.lintrk.q.push([a, b]);
        window.lintrk.q = [];
    }
    const s = document.getElementsByTagName('script')[0];

    let b = document.createElement('script');

    b.type = 'text/javascript';
    b.async = true;
    b.src = 'https://snap.licdn.com/li.lms-analytics/insight.min.js';

    s.parentNode.insertBefore(b, s);
}

export const vendorName = 'LinkedIn Pixel';
export const cmpVendorId = '5f2d22a6b8e05c02aa283b3c';
