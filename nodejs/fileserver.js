// Based on http://code.tutsplus.com/tutorials/how-to-create-a-resumable-video-uploade-in-node-js--net-25445
// Modifed to support Android by Aruna Tennakoon

var app = require('http').createServer(handler)
  , io = require('socket.io').listen(app)
  , fs = require('fs')
  , exec = require('child_process').exec
  , util = require('util')
 
var Files = {};

io.set('log level', 1);
app.listen(8090);
 
function handler (req, res) {
  fs.readFile(__dirname + '/index.html',
  function (err, data) {
    if (err) {
      res.writeHead(500);
      return res.end('Error loading index.html');
    }
    res.writeHead(200);
    res.end(data);
  });
}
 
io.sockets.on('connection', function (socket) {
    socket.on('uploadFileStart', function (data) { //data contains the variables that we passed through in the html file
        var fileName = data['Name'];
        var fileSize = data['Size'];
        var Place = 0;

        var uploadFilePath = 'Temp/' + fileName;

        console.log('uploadFileStart # Uploading file: %s to %s. Complete file size: %d', fileName, uploadFilePath, fileSize);

        Files[fileName] = {  //Create a new Entry in The Files Variable
            FileSize    : fileSize,
            Data        : "",
            Downloaded  : 0
        }        

        fs.open(uploadFilePath, "a", 0755, function(err, fd){
            if(err) {
                console.log(err);
            }
            else {
                console.log('uploadFileStart # Requesting Place: %d Percent %d', Place, 0);

                Files[fileName]['Handler'] = fd; //We store the file handler so we can write to it later
                socket.emit('uploadFileMoreDataReq', { 'Place' : Place, 'Percent' : 0 });

                // Send webclient upload progress..
            }
        });
    });

    socket.on('uploadFileChuncks', function (data){
        var Name = data['Name'];
        var base64Data = data['Data'];
        var playload = new Buffer(base64Data, 'base64').toString('binary');

        console.log('uploadFileChuncks # Got name: %s, received chunk size %d.', Name, playload.length);

        Files[Name]['Downloaded'] += playload.length;
        Files[Name]['Data'] += playload;        
        
        if(Files[Name]['Downloaded'] == Files[Name]['FileSize']) //If File is Fully Uploaded
        {

            console.log('uploadFileChuncks # File %s receive completed', Name);

            fs.write(Files[Name]['Handler'], Files[Name]['Data'], null, 'Binary', function(err, Writen){
               // close the file
               fs.close(Files[Name]['Handler'], function() {
                  console.log('file closed');
               });

                // Notify android client we are done.
                socket.emit('uploadFileCompleteRes', { 'IsSuccess' : true });

                // Send the Webclient the path to download this file.
                
            });
        }
        else if(Files[Name]['Data'].length > 10485760){ //If the Data Buffer reaches 10MB
            console.log('uploadFileChuncks # Updating file %s with received data', Name);

            fs.write(Files[Name]['Handler'], Files[Name]['Data'], null, 'Binary', function(err, Writen){
                Files[Name]['Data'] = ""; //Reset The Buffer
                var Place = Files[Name]['Downloaded'];
                var Percent = (Files[Name]['Downloaded'] / Files[Name]['FileSize']) * 100;

                socket.emit('uploadFileMoreDataReq', { 'Place' : Place, 'Percent' :  Percent});

                // Send webclient upload progress..

            });
        }
        else
        {
            var Place = Files[Name]['Downloaded'];
            var Percent = (Files[Name]['Downloaded'] / Files[Name]['FileSize']) * 100;
            console.log('uploadFileChuncks # Requesting Place: %d, Percent %s', Place, Percent);

            socket.emit('uploadFileMoreDataReq', { 'Place' : Place, 'Percent' :  Percent});
            // Send webclient upload progress..
        }
    });
});