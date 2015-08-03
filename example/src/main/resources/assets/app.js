
var QuarterCircle = function(paper) {
    var start = new paper.Point(0, 0);
    var through = new paper.Point(Math.cos(Math.PI * 0.25), 1 - Math.sin(Math.PI * 0.25));
    var end = new paper.Point(1, 1);
    var arc = new paper.Path.Arc(start, through, end);
    arc.strokeColor = 'black';
    return arc;
};

var Box = function(paper) {
    var topLeft = new paper.Point(0, 0);
    var bottomRight = new paper.Point(1, 1);
    var rect = new paper.Path.Rectangle(topLeft, bottomRight);
    rect.strokeColor = 'black';
    rect.dashArray = [10, 4];
    return rect;
}

var Scene = function(paper) {
    var circle = QuarterCircle(paper);
    var box = Box(paper);
    return circle.join(box);
}

var scene = Scene(paper);
var height = view.size.height - (view.size.height * 0.20);
scene.scale(height);
scene.position = view.center;
// Draw the view now:
var text = new PointText({
    point: [50, 50],
    content: 'The contents of the point text',
    fillColor: 'black',
    fontFamily: 'Courier New',
    fontWeight: 'bold',
    fontSize: 25
});
text.content = 'And now something different';
view.draw();

