// Activation des variables CSS
cssVars({
    rootElement: document, // default
    onlyLegacy: true
});

/////////// Affichage d'un element de toast dans les notifications

$('.toast').toast({
    'animation': true,
    'autohide': false
});
$('.toast').toast('show');

/////// Carousel

var $carouselIndicators = $(".carousel-bullet > li");
$('#helpCarousel').on('slide.bs.carousel', function () {
    setTimeout(function () {
        var currentSlideNo = $('.carousel-item.active').index('.carousel-item'),
            newItem = $(".carousel-bullet > li[data-slide-to='" + currentSlideNo + "']");
        $carouselIndicators.removeClass("active");
        newItem.addClass("active");
    }, 350)

});



//////// Toggle des sidebars de la sidebar

$('#notificationsCollapse').on('click', function () {
    $('#sidebar,#content').toggleClass('shrinked');
    $('#notifications').toggleClass('expanded');
    $('#notificationsCollapse').toggleClass('new');
});

$('#link-profil').on('click', function () {
    $('#content').toggleClass('hide');
    $('#profil').toggleClass('hide');
});

$('#nav-home').on('click', function () {
    $('#content').toggleClass('hide');
    $('#profil').toggleClass('hide');
});

$('#nav-menu').on('click', function () {
    $('#nav-menu').toggleClass('show');
    $('#menu').toggleClass('hide');
    $('.overlay').toggleClass('active');
    $('#tutorial,#content').removeClass('show');
    $('#sidebar,#content').removeClass('shrinked');

});

$('#nav-help').on('click', function () {
    $('#sidebar,#content').toggleClass('shrinked');
    $('#tutorial,#content').toggleClass('show');
});

$('#closeTutorial').on('click', function () {
    $('#content').toggleClass('shrinked');
    $('#sidebar').toggleClass('shrinked');
    $('#tutorial').toggleClass('show');
    $('#content').toggleClass('show');
});

$('.close-tuto').on('click', function () {
    $('#sidebar,#content').toggleClass('shrinked');
    $('#tutorial,#content').removeClass('show');
});

$('.overlay').on('click', function () {
    $('.overlay').removeClass('active');
    $('#menu').addClass('hide');
    $('#nav-menu').removeClass('show');

});

$('#menu .card-link').on('click', function () {
    $('.overlay').removeClass('active');
    $('#menu').addClass('hide');
    $('#nav-menu').removeClass('show');
});




//Waypoints des notifications
var waypoints = $('.notification').waypoint({
    handler: function () {
    },
    handler: function (direction) {
        if (direction == 'down') {
            $('.notification').addClass('shrink');

        } else {
            $('.notification').removeClass('shrink');

        }
    },
    context: '.notifyer',
    offset: '-20%'
});

/*
$(".table-wrapper").mousewheel(function(event, delta) {
    this.scrollLeft -= (delta * 80);
    this.scrollRight -= (delta * 80);
    event.preventDefault();
});
*/
