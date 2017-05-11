// ----- Imports ----- //

import URLSearchParams from 'URLSearchParams';


// ----- Setup ----- //

const SELECTED_CONTRIB = 'contribution-fields__field--selected';
const SELECTED_PRINT = 'print-fields__field--selected';
const SHOW_DETAILS = 'show-details';
const MONTHLY_URL = '/monthly-contribution';
const ONEOFF_URL = 'https://contribute.theguardian.com/uk';
const DIGI_URL = 'https://subscribe.theguardian.com/p/DXX83X';
const PRINT_DIGI_URL = 'https://subscribe.theguardian.com/p/GXX83X';
const PRINT_URL = 'https://subscribe.theguardian.com/p/GXX83P';
const CONTRIB_ERROR = 'contribution-error--shown';
const DEFAULT_INTCMP = 'gdnwb_copts_bundles_landing_default';

const STATE = {
	contribAmount: '5',
	contribPeriod: 'MONTHLY',
	printBundle: 'PRINT+DIGITAL',
	amountButton: 'ONE',
	intcmp: null,
	printUrl: PRINT_URL,
	printDigiUrl: PRINT_DIGI_URL
};

const PRICES = {
	MONTHLY: {
		ONE: '5',
		TWO: '10',
		THREE: '20'
	},
	ONE_OFF: {
		ONE: '25',
		TWO: '50',
		THREE: '100'
	}
};

const ERRORS = {
    tooLittleOneOff: 'Please enter at least £1',
    tooLittle: 'Please enter at least £5',
    tooMuch: 'We are presently only able to accept contributions of £2000 or less',
    badInput: 'Please enter a numeric amount',
    noEntry: 'Please enter an amount'
};


// ----- Functions ----- //

// Sets the link to contributions to include INTCMP,
// and based upon billing period and amount.
function contribLink (elems) {

	let params = new URLSearchParams();
	params.append('INTCMP', STATE.intcmp);

	if (STATE.contribPeriod === 'MONTHLY') {
		params.append('contributionValue', STATE.contribAmount);
		elems.contribLink.href = `${MONTHLY_URL}?${params.toString()}`;
	} else {
		params.append('amount', STATE.contribAmount);
		elems.contribLink.href = `${ONEOFF_URL}?${params.toString()}`;
	}

}

// Sets the links to subscriptions to include INTCMP,
// and based upon bundle selected.
function subsLinks (elems) {

	let params = new URLSearchParams();
	params.append('INTCMP', STATE.intcmp);

	const digiUrl = `${DIGI_URL}?${params.toString()}`;
	STATE.printDigiUrl = `${PRINT_DIGI_URL}?${params.toString()}`;
	STATE.printUrl = `${PRINT_URL}?${params.toString()}`;

	elems.digiLink.href = digiUrl;
	elems.printLink.href = STATE.printDigiUrl;

}

// Sets the patrons and events links to include INTCMP.
function otherLinks (elems) {

	let params = new URLSearchParams();
	params.append('INTCMP', STATE.intcmp);

	elems.patrons.href = `${elems.patrons.href}?${params.toString()}`;
	elems.events.href = `${elems.events.href}?${params.toString()}`;

}

// Updates all the links on the page based on INTCMP.
function updateLinks (elems) {

	let currentParams = new URLSearchParams(window.location.search.slice(1));
	let campaignCode = currentParams.get('INTCMP');

	STATE.intcmp = campaignCode ? campaignCode : DEFAULT_INTCMP;

	contribLink(elems);
	subsLinks(elems);
	otherLinks(elems);

}

// Updates the contributions amount in the state object, and changes the
// possible amount based on billing period.
function contribUpdate (elems) {

	let amount = PRICES[STATE.contribPeriod][STATE.amountButton];
	STATE.contribAmount = amount ? amount : elems.contribOther.value;

	elems.contribOne.textContent = `£${PRICES[STATE.contribPeriod].ONE}`;
	elems.contribTwo.textContent = `£${PRICES[STATE.contribPeriod].TWO}`;
	elems.contribThree.textContent = `£${PRICES[STATE.contribPeriod].THREE}`;

	contribLink(elems);

}

// Updates the print link and digital benefits bullet point based on bundle
// selected.
function printUpdate (elems, bundle) {

	if (bundle === 'PRINT') {
		elems.printLink.href = STATE.printUrl;
		elems.digitalBenefits.classList.add('is-hidden');
	} else {
		elems.printLink.href = STATE.printDigiUrl;
		elems.digitalBenefits.classList.remove('is-hidden');
	}

}

// Displays an error for the contributions (other amount) input.
function contribError (elems, errorType) {

	if (errorType) {
		elems.contribError.textContent = ERRORS[errorType];
		elems.contribError.classList.add(CONTRIB_ERROR);
	} else {
		elems.contribError.classList.remove(CONTRIB_ERROR);
	}

}

// Checks that the other amount field is valid.
function validateOtherAmount (elems) {

	elems.contribLink.addEventListener('click', (event) => {

		event.preventDefault();

		if (STATE.contribAmount === '') {
			contribError(elems, 'noEntry');
			return;
		}

		let amount = Number(STATE.contribAmount);

		if (isNaN(amount)) {
			contribError(elems, 'badInput');
		} else if (amount < 5 && STATE.contribPeriod === 'MONTHLY') {
			contribError(elems, 'tooLittle');
		} else if (amount < 1 && STATE.contribPeriod === 'ONE_OFF') {
			contribError(elems, 'tooLittleOneOff');
		} else if (amount > 2000) {
			contribError(elems, 'tooMuch');
		} else {
			window.location.assign(elems.contribLink.href);
		}

	});

}

// Handles clicks on the contributions bundle element on the page.
function contributionClicks (elems) {

	elems.contribMonth.addEventListener('click', () => {
		contribError(elems, false);
		elems.contribMonth.classList.add(SELECTED_CONTRIB);
		elems.contribOneoff.classList.remove(SELECTED_CONTRIB);
		STATE.contribPeriod = 'MONTHLY';
		contribUpdate(elems);
	});

	elems.contribOneoff.addEventListener('click', () => {
		contribError(elems, false);
		elems.contribOneoff.classList.add(SELECTED_CONTRIB);
		elems.contribMonth.classList.remove(SELECTED_CONTRIB);
		STATE.contribPeriod = 'ONE_OFF';
		contribUpdate(elems);
	});

}

// Changes the visual appearance of the selected contribution amount.
function selectAmount (elems, selectedElem) {

	let amountElems = [elems.contribOne, elems.contribTwo, elems.contribThree,
		elems.contribOther];

	amountElems.filter(elem => elem !== selectedElem).map(elem => {
		elem.classList.remove(SELECTED_CONTRIB);
	});

	selectedElem.classList.add(SELECTED_CONTRIB);

}

// Handles clicks on the predefined contribution amount elements.
function amountClicks (elems) {

	elems.contribOne.addEventListener('click', () => {

		contribError(elems, false);
		STATE.amountButton = 'ONE';
		selectAmount(elems, elems.contribOne);
		contribUpdate(elems);

	});

	elems.contribTwo.addEventListener('click', () => {

		contribError(elems, false);
		STATE.amountButton = 'TWO';
		selectAmount(elems, elems.contribTwo);
		contribUpdate(elems);

	});

	elems.contribThree.addEventListener('click', () => {

		contribError(elems, false);
		STATE.amountButton = 'THREE';
		selectAmount(elems, elems.contribThree);
		contribUpdate(elems);

	});

}

// Handles clicks on the contribution other amount element.
function otherAmountEvents (elems) {

	elems.contribOther.addEventListener('click', () => {

		STATE.amountButton = 'OTHER';
		selectAmount(elems, elems.contribOther);
		contribUpdate(elems);

	});

	elems.contribOther.addEventListener('input', () => {

		STATE.contribAmount = elems.contribOther.value;
		contribError(elems, false);
		contribUpdate(elems);

	});

}

// Handles clicks on the print bundle buttons.
function printClicks (elems) {

	elems.print.addEventListener('click', () => {
		elems.print.classList.add(SELECTED_PRINT);
		elems.printDigital.classList.remove(SELECTED_PRINT);
		STATE.printBundle = 'PRINT';
		printUpdate(elems, STATE.printBundle);
	});

	elems.printDigital.addEventListener('click', () => {
		elems.printDigital.classList.add(SELECTED_PRINT);
		elems.print.classList.remove(SELECTED_PRINT);
		STATE.printBundle = 'PRINT+DIGITAL';
		printUpdate(elems, STATE.printBundle);
	});

}

// Retrieves an object of references to the DOM elements in the page.
function getElems () {

	return {
		contribMonth: document.getElementsByClassName('js-contribution-monthly')[0],
		contribOneoff: document.getElementsByClassName('js-contribution-oneoff')[0],
		contribOne: document.getElementsByClassName('js-contribution-one')[0],
		contribTwo: document.getElementsByClassName('js-contribution-two')[0],
		contribThree: document.getElementsByClassName('js-contribution-three')[0],
		contribOther: document.getElementsByClassName('js-contribution-other')[0],
		contribError: document.getElementsByClassName('js-contribution-error')[0],
		print: document.getElementsByClassName('js-print-paper')[0],
		printDigital: document.getElementsByClassName('js-print-paper-digital')[0],
		contribLink: document.getElementsByClassName('js-contrib-link')[0],
		digiLink: document.getElementsByClassName('js-digi-link')[0],
		printLink: document.getElementsByClassName('js-print-link')[0],
		digitalBenefits: document.getElementsByClassName('js-digital-benefits')[0],
		patrons: document.getElementsByClassName('js-patrons')[0],
		events: document.getElementsByClassName('js-events')[0]
	};

}


// ----- Export ----- //

export function init () {

	if (guardian.membership.bundlesLanding) {

		let elems = getElems();

		updateLinks(elems);
		contributionClicks(elems);
		validateOtherAmount(elems);
		amountClicks(elems);
		printClicks(elems);
		otherAmountEvents(elems);

	}

}
