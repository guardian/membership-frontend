    import ajax from 'ajax';
    import raven from 'src/modules/raven';
    import analytics from 'src/modules/analytics/setup';
    import welcome from 'src/modules/welcome';
    import slideshow from 'src/modules/slideshow';
    import images from 'src/modules/images';
    import toggle from 'src/modules/toggle';
    import dropdown from 'src/modules/dropdown';
    import sticky from 'src/modules/sticky';
    import sectionNav from 'src/modules/sectionNav';
    import navigation from 'src/modules/navigation';
    import userDetails from 'src/modules/userDetails';
    import videoOverlay from 'src/modules/videoOverlay';
    import modal from 'src/modules/modal';
    import cta from 'src/modules/events/cta';
    import remainingTickets from 'src/modules/events/remainingTickets';
    import eventPriceEnhance from 'src/modules/events/eventPriceEnhance';
    import filterFacets from 'src/modules/filterFacets';
    import filterLiveSearch from 'src/modules/filterLiveSearch';
    import form from 'src/modules/form';
    import processSubmit from 'src/modules/form/processSubmit';
    import identityPopup from 'src/modules/identityPopup';
    import identityPopupDetails from 'src/modules/identityPopupDetails';
    import * as comparisonTable from 'src/modules/comparisonTable';
    import metrics from 'src/modules/metrics';
    import patterns from 'src/modules/patterns';
    import * as giraffe from 'src/modules/giraffe';
    import * as paidToPaid from 'src/modules/paidToPaid';


        ajax.init({page: {ajaxUrl: ''}});
        raven.init('https://8ad435f4fefe468eb59b19fd81a06ea9@app.getsentry.com/56405');

        analytics.init();

        // Global
        welcome.init();
        slideshow.init();
        images.init();
        toggle.init();
        dropdown.init();
        sticky.init();
        sectionNav.init();
        identityPopup.init();
        identityPopupDetails.init();
        navigation.init();
        userDetails.init();
        videoOverlay.init();
        modal.init();
        comparisonTable.init();

        // Events
        cta.init();
        remainingTickets.init();
        eventPriceEnhance.init();

        // Filtering
        filterFacets.init();
        filterLiveSearch.init();

        // Forms
        form.init();
        processSubmit.init();

        // Metrics
        metrics.init();

        // Pattern library
        patterns.init();

        paidToPaid.init();


