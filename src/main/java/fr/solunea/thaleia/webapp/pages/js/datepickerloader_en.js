$(document).ready(function(){
	moment.locale('en');
	if ($('#daterange').daterangepicker != null){
		$('#daterange').daterangepicker({
			opens: 'left',
			format: 'MM/DD/YYYY',
			endDate: moment(),
			locale:{
	            applyLabel: 'Apply',
	            cancelLabel: 'Cancel',
	            fromLabel: 'From',
	            toLabel: 'to',
	            weekLabel: 'W',
	            customRangeLabel: 'Date range',
	            daysOfWeek: moment.weekdaysMin(),
	            monthNames: moment.monthsShort(),
	            firstDay: moment.localeData()._week.dow
	        }
		});
	}
});