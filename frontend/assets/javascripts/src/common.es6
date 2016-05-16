import ajax from 'ajax';
import raven from 'src/modules/raven';
import analytics from 'src/modules/analytics/setup';
import images from 'src/modules/images';
import navigation from 'src/modules/navigation';
import userDetails from 'src/modules/userDetails';
import form from 'src/modules/form';
import processSubmit from 'src/modules/form/processSubmit';
import identityPopup from 'src/modules/identityPopup';
import identityPopupDetails from 'src/modules/identityPopupDetails';
import metrics from 'src/modules/metrics';
import dropdown from 'src/modules/dropdown';

export default {
    init: () => {
        ajax.init({page: {ajaxUrl: ''}});
        raven.init('https://8ad435f4fefe468eb59b19fd81a06ea9@app.getsentry.com/56405');

        analytics.init();

        // Global
        images.init();
        dropdown.init();
        identityPopup.init();
        identityPopupDetails.init();
        navigation.init();
        userDetails.init();

        // Events
        cta.init();

        // Forms
        form.init();
        processSubmit.init();

        // Metrics
        metrics.init();
    }
}


