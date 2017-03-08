// ----- Setup ----- //

const SELECTED_CONTRIB = 'contribution-fields__field--selected';
const SELECTED_PRINT = 'print-fields__field--selected';
const SHOW_DETAILS = 'show-details';
const MONTHLY_URL = '/monthly-contribution';
const ONEOFF_URL = 'https://contribute.theguardian.com/uk';
const PRINT_DIGI_URL = 'https://subscribe.theguardian.com/collection/paper-digital?INTCMP=GU_SIMPLE_UK_PAPER_DIGITAL';
const PRINT_URL = 'https://subscribe.theguardian.com/collection/paper?INTCMP=GU_SIMPLE_UK_PAPER';
const CONTRIB_ERROR = 'contribution-error--shown';

let STATE = {
	contribAmount: '5',
	contribPeriod: 'MONTHLY',
	printBundle: 'PRINT+DIGITAL',
	amountButton: 'ONE'
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
	tooLittle: 'Please enter at least £5',
	tooMuch: 'We are presently only able to accept contributions of £2000 or less',
	badInput: 'Please enter a numeric amount',
	noEntry: 'Please enter an amount'
};


// ----- Functions ----- //

function contribUpdate (elems) {

	let amount = PRICES[STATE.contribPeriod][STATE.amountButton];
	STATE.contribAmount = amount ? amount : elems.contribOther.value;

	elems.contribOne.textContent = `£${PRICES[STATE.contribPeriod].ONE}`;
	elems.contribTwo.textContent = `£${PRICES[STATE.contribPeriod].TWO}`;
	elems.contribThree.textContent = `£${PRICES[STATE.contribPeriod].THREE}`;

	if (STATE.contribPeriod === 'MONTHLY') {
		elems.contribLink.href = `${MONTHLY_URL}?contributionValue=${STATE.contribAmount}`;
	} else {
		elems.contribLink.href = `${ONEOFF_URL}?amount=${STATE.contribAmount}`;
	}

}

function printUpdate (elems, bundle) {

	if (bundle === 'PRINT') {
		elems.printLink.href = PRINT_URL;
		elems.digitalBenefits.classList.add('is-hidden');
	} else {
		elems.printLink.href = PRINT_DIGI_URL;
		elems.digitalBenefits.classList.remove('is-hidden');
	}

}

function contribError (elems, errorType) {

	if (errorType) {
		elems.contribError.textContent = ERRORS[errorType];
		elems.contribError.classList.add(CONTRIB_ERROR);
	} else {
		elems.contribError.classList.remove(CONTRIB_ERROR);
	}

}

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
		} else if (amount > 2000) {
			contribError(elems, 'tooMuch');
		} else {
			window.location.assign(elems.contribLink.href);
		}

	});

}

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

function selectAmount (elems, selectedElem) {

	let amountElems = [elems.contribOne, elems.contribTwo, elems.contribThree,
		elems.contribOther];

	amountElems.filter(elem => elem !== selectedElem).map(elem => {
		elem.classList.remove(SELECTED_CONTRIB);
	});

	selectedElem.classList.add(SELECTED_CONTRIB);

}

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

function detailsClicks (elems) {

	elems.digitalMore.addEventListener('click', () => {
		if (elems.digitalDetails.classList.contains(SHOW_DETAILS)) {
			elems.digitalDetails.classList.remove(SHOW_DETAILS);
		} else {
			elems.digitalDetails.classList.add(SHOW_DETAILS);
		}
	});

	elems.printMore.addEventListener('click', () => {
		if (elems.printDetails.classList.contains(SHOW_DETAILS)) {
			elems.printDetails.classList.remove(SHOW_DETAILS);
		} else {
			elems.printDetails.classList.add(SHOW_DETAILS);
		}
	});

}

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
		printLink: document.getElementsByClassName('js-print-link')[0],
		digitalBenefits: document.getElementsByClassName('js-digital-benefits')[0],
		digitalMore: document.getElementsByClassName('js-digital-more')[0],
		digitalDetails: document.getElementsByClassName('js-digital-details')[0],
		printMore: document.getElementsByClassName('js-print-more')[0],
		printDetails: document.getElementsByClassName('js-print-details')[0]
	};

}


// ----- Export ----- //

export function init () {

	if (guardian.membership.bundlesLanding) {

		let elems = getElems();

		contributionClicks(elems);
		validateOtherAmount(elems);
		amountClicks(elems);
		printClicks(elems);
		otherAmountEvents(elems);
		detailsClicks(elems);

	}

}
