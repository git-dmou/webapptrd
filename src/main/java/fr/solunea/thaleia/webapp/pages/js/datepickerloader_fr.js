$(document).ready(function(){
	moment.locale('fr');
	if ($('#daterange').daterangepicker != null){
		$('#daterange').daterangepicker({
			opens: 'left',
			format: 'DD/MM/YYYY',
			endDate: moment(),
			locale:{
	            applyLabel: 'Appliquer',
	            cancelLabel: 'Annuler',
	            fromLabel: 'De',
	            toLabel: 'à',
	            weekLabel: 'S',
	            customRangeLabel: 'Periode',
	            daysOfWeek: moment.weekdaysMin(),
	            monthNames: moment.monthsShort(),
	            firstDay: moment.localeData()._week.dow
	        }
		});
	}
});