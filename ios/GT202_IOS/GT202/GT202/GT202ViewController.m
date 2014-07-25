//
//  GT202ViewController.m
//  GT202
//
//  Created by Edmond Leung on 12/5/14.
//  Edited by Derek Li on 20140725
//  Copyright (c) 2014 Arrow. All rights reserved.
//

#import "GT202ViewController.h"
#import "GT202TemperatureViewController.h"
#import "GT202HumidityViewController.h"
#import "GT202RemoteControlViewController.h"

#include <CoreFoundation/CoreFoundation.h>
#include <sys/socket.h>
#include <netinet/in.h>

#include <ifaddrs.h>
#include <arpa/inet.h>

@interface GT202ViewController ()
@property (weak, nonatomic) IBOutlet UIButton *btnLightBulb;
@property (weak, nonatomic) IBOutlet UITextView *debugText;

@end

@implementation GT202ViewController

const Byte cmdGetTemperature = 0x00;
const Byte cmdGetHumidity = 0x01;
const Byte cmdTurnOnLED = 0x02;
const Byte cmdTurnOffLED = 0x03;
const Byte cmdGetAll = 0xfe;

const Byte cmdDVDNumber0 = 0x10;
const Byte cmdDVDNumber1 = 0x11;
const Byte cmdDVDNumber2 = 0x12;
const Byte cmdDVDNumber3 = 0x13;
const Byte cmdDVDNumber4 = 0x14;
const Byte cmdDVDNumber5 = 0x15;
const Byte cmdDVDNumber6 = 0x16;
const Byte cmdDVDNumber7 = 0x17;
const Byte cmdDVDNumber8 = 0x18;
const Byte cmdDVDNumber9 = 0x19;
const Byte cmdDVDUp = 0x20;
const Byte cmdDVDDown = 0x21;
const Byte cmdDVDLeft = 0x22;
const Byte cmdDVDRight = 0x23;
const Byte cmdDVDOk = 0x24;
const Byte cmdDVDSet = 0x25;
const Byte cmdDVDBack = 0x26;
const Byte cmdDVDPower = 0x27;
const Byte cmdDVDMute = 0x28;
const Byte cmdDVDPlay = 0x29;
const Byte cmdDVDPause = 0x30;
const Byte cmdDVDStop = 0x31;
const Byte cmdDVDNext = 0x32;
const Byte cmdDVDPrev = 0x33;
const Byte cmdDVDFF = 0x34;
const Byte cmdDVDFB = 0x35;

const int PACKET_LENGTH = 16;
const Byte MAC_ANY[] = { 0x00, 0x00, 0x00, 0x00, 0x00, 0x00 };


bool isLightOn = false;
bool isConnectedToDevice = false;

NSString *localIp;

int multicastSocket = -1;
CFRunLoopSourceRef runLoopSource;

NSMutableArray *arrayMac;

- (void)viewDidLoad
{
    [super viewDidLoad];
	// Do any additional setup after loading the view, typically from a nib.

    globalSelf = self;
    
    // background image
    UIGraphicsBeginImageContext(self.view.frame.size);
    [[UIImage imageNamed:@"background_dim.png"] drawInRect:self.view.bounds];
    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    self.view.backgroundColor = [UIColor colorWithPatternImage:image];
    
    localIp = [self getLocalIpAddressString];
    [_debugText setText:@""];
    [self updateMacPicker:MAC_ANY];
    [self prepareMulticast];
    [self startMyTimer];
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    if ([[segue identifier] isEqualToString:@"SEGUE_TO_TEMPERATURE"])
    {
        // Get reference to the destination view controller
        UINavigationController *n_vc = [segue destinationViewController];
        GT202TemperatureViewController *vc = (GT202TemperatureViewController *)n_vc.viewControllers[0];

        // Pass any objects to the view controller here, like...
        vc.home = self;
    }
    else if ([[segue identifier] isEqualToString:@"SEGUE_TO_HUMIDITY"])
    {
        // Get reference to the destination view controller
        UINavigationController *n_vc = [segue destinationViewController];
        GT202HumidityViewController *vc = (GT202HumidityViewController *)n_vc.viewControllers[0];
        
        // Pass any objects to the view controller here, like...
        vc.home = self;
    }
    else if ([[segue identifier] isEqualToString:@"SEGUE_TO_REMOTE_CONTROL"])
    {
        // Get reference to the destination view controller
        UINavigationController *n_vc = [segue destinationViewController];
        GT202RemoteControlViewController *vc = (GT202RemoteControlViewController *)n_vc.viewControllers[0];
        
        // Pass any objects to the view controller here, like...
        vc.home = self;
    }
}

- (IBAction)unwindToHome:(UIStoryboardSegue *)segue
{
    
}

-(void) multicastDataProcess:(NSData *)multicastData
{
    if([multicastData length] != PACKET_LENGTH)
        return;
    
    Byte data[PACKET_LENGTH];

    [multicastData getBytes:data length:PACKET_LENGTH];
  
    if ((data[0] & 0xFF) != 0xAA) {
        /*NSString *preamble = [NSString stringWithFormat:@"0x%02X", data[0] & 0xFF];
        [self debugTextAppend:@"Invalid Packet received, started with: %@", preamble];
        NSLog(@"Invalid Packet received, started with: %@", preamble);*/
        
        return;
    }

    NSMutableString *hexStr = [NSMutableString stringWithCapacity:PACKET_LENGTH];
    for (int i=0; i < PACKET_LENGTH; i++) {
        [hexStr appendFormat: @"%02X ", data[i]];
    }
    
    // validate CRC byte
    NSUInteger crc = 0;
    for (int i = 0; i < (PACKET_LENGTH - 1); i++) {
        crc += (data[i] & 0xFF);
    }
    if (crc % 256 != (data[PACKET_LENGTH - 1] & 0xFF)) {
        [self debugTextAppend:@"Invalid Packet received, CRC error: %@", hexStr];
        NSLog(@"Invalid Packet received, CRC error: %@", hexStr);
        
        return;
    }
    
    [self debugTextAppend:@"Data received: %@", hexStr];
    NSLog(@"Data received: %@", hexStr);
    
    // CRC validated, complete 16-byte command sequence
    
    Byte macAddress[6];
    for (int i = 0; i < 6; i++) {
        macAddress[i] = (Byte)data[i+1];
    }
    
    [self appendMacPicker:macAddress];
    
    Byte cmdType = data[7];
    
    switch (cmdType) {
        case cmdGetTemperature: {
            // Get Temperature
            NSData *tmp = [NSData dataWithBytes:&data[9] length:2];
            UInt16 temperature = *(const UInt16 *)[tmp bytes];
            
            NSString *nssTemp = [NSString stringWithFormat:@"%dºC", temperature];
            [[self textViewTemperature] setText:nssTemp];
            
            [self debugTextAppend:@"Updated Temperature to %@", nssTemp];
            
            break;
        }
        case cmdGetHumidity: {
            // Get Humidity
            NSData *tmp = [NSData dataWithBytes:&data[11] length:2];
            UInt16 humidity = *(const UInt16 *)[tmp bytes];
            
            NSString *nssHum = [NSString stringWithFormat:@"%d%%", humidity];
            [[self textViewHumidity] setText:nssHum];
            
            [self debugTextAppend:@"Updated Humidity to %@", nssHum];
            
            break;
        }
        case cmdTurnOnLED: {
            // ACK - Turn on LED
            UIImage *btnImage = [UIImage imageNamed:@"lightbulb_on.png"];
            [_btnLightBulb setImage:btnImage forState:UIControlStateNormal];
                
            UIGraphicsBeginImageContext(self.view.frame.size);
            [[UIImage imageNamed:@"background.png"] drawInRect:self.view.bounds];
            UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();
            self.view.backgroundColor = [UIColor colorWithPatternImage:image];
                
            isLightOn = true;
                
            [self debugTextAppend:@"ACK: LED Turned on!"];
            
            break;
        }
        case cmdTurnOffLED: {
            // ACK - Turn off LED
            UIImage *btnImage = [UIImage imageNamed:@"lightbulb_off.png"];
            [_btnLightBulb setImage:btnImage forState:UIControlStateNormal];
                
            UIGraphicsBeginImageContext(self.view.frame.size);
            [[UIImage imageNamed:@"background_dim.png"] drawInRect:self.view.bounds];
            UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
            UIGraphicsEndImageContext();
            self.view.backgroundColor = [UIColor colorWithPatternImage:image];
                
            isLightOn = false;
                
            [self debugTextAppend:@"ACK: LED Turned off!"];
            
            break;
        }
        case cmdGetAll: {
            // Get Humidity & Temperature
            NSData *tmp = [NSData dataWithBytes:&data[8] length:1];
            Byte ledStatus = *(const Byte *)[tmp bytes];

            tmp = [NSData dataWithBytes:&data[9] length:2];
            UInt16 temperature = *(const UInt16 *)[tmp bytes];
            
            tmp = [NSData dataWithBytes:&data[11] length:2];
            UInt16 humidity = *(const UInt16 *)[tmp bytes];
            
            NSString *nssTemp = [NSString stringWithFormat:@"%dºC", temperature];
            [[self textViewTemperature] setText:nssTemp];
            [self debugTextAppend:@"Updated Temperature to %@", nssTemp];
            
            NSString *nssHum = [NSString stringWithFormat:@"%d%%", humidity];
            [[self textViewHumidity] setText:nssHum];
            [self debugTextAppend:@"Updated Humidity to %@", nssHum];
            
            if(ledStatus == 0x01) // led off
            {
                if (isLightOn == true) {
                    UIImage *btnImage = [UIImage imageNamed:@"lightbulb_off.png"];
                    [_btnLightBulb setImage:btnImage forState:UIControlStateNormal];
                    
                    UIGraphicsBeginImageContext(self.view.frame.size);
                    [[UIImage imageNamed:@"background_dim.png"] drawInRect:self.view.bounds];
                    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
                    UIGraphicsEndImageContext();
                    self.view.backgroundColor = [UIColor colorWithPatternImage:image];
                    
                    isLightOn = false;
                    
                    [self debugTextAppend:@"ACK: LED Turned off!"];
                }
            }
            else // led on
            {
                if (isLightOn == false) {
                    UIImage *btnImage = [UIImage imageNamed:@"lightbulb_on.png"];
                    [_btnLightBulb setImage:btnImage forState:UIControlStateNormal];
                    
                    UIGraphicsBeginImageContext(self.view.frame.size);
                    [[UIImage imageNamed:@"background.png"] drawInRect:self.view.bounds];
                    UIImage *image = UIGraphicsGetImageFromCurrentImageContext();
                    UIGraphicsEndImageContext();
                    self.view.backgroundColor = [UIColor colorWithPatternImage:image];
                    
                    isLightOn = true;
                    
                    [self debugTextAppend:@"ACK: LED Turned on!"];
                }
            }
            
            break;
        }
        default:
            break;
    }
}

void handleNetworkData(CFSocketRef socket, CFSocketCallBackType type, CFDataRef address, const void *data, void *info)
{
    switch (type) {
        case kCFSocketDataCallBack:
            {
                NSData *multicastData = (__bridge NSData*)data;
                GT202ViewController *obj;
                obj = (__bridge GT202ViewController *) info;
                [obj multicastDataProcess:multicastData];
            }
            break;
        default:
            break;
    }
}

- (void)prepareMulticast
{
    int sock_reuse = 1;
    struct ip_mreq multicast;
    struct sockaddr_in recver_addr;
    
    memset(&multicast, 0, sizeof(multicast));
    memset(&recver_addr, 0, sizeof(recver_addr));
    multicast.imr_multiaddr.s_addr = inet_addr("224.2.2.2");
    multicast.imr_interface.s_addr = htonl(INADDR_ANY);
    recver_addr.sin_family = AF_INET;
    recver_addr.sin_port = htons(5111);
    recver_addr.sin_addr.s_addr = INADDR_ANY;
    
    if((multicastSocket = socket(AF_INET, SOCK_DGRAM, 0)) < 0) {
        [self debugTextAppend:@"Multicast Socket failed at %@:%@", @"224.2.2.2", @"5111"];
        multicastSocket = -1;
        return;
    }
    
    if(setsockopt(multicastSocket, SOL_SOCKET, SO_REUSEADDR, (char *)&sock_reuse, sizeof(sock_reuse)) < 0) {
        [self debugTextAppend:@"Multicast Socket failed at %@:%@", @"224.2.2.2", @"5111"];
        multicastSocket = -1;
        return;
    }

    if(bind(multicastSocket, (struct sockaddr*)&recver_addr, sizeof(recver_addr)) < 0 ) {
        [self debugTextAppend:@"Multicast Socket failed at %@:%@", @"224.2.2.2", @"5111"];
        multicastSocket = -1;
        return;
    }

    if(setsockopt(multicastSocket, IPPROTO_IP, IP_ADD_MEMBERSHIP, (char *)&multicast, sizeof(multicast)) != 0 ) {
        [self debugTextAppend:@"Multicast Socket failed at %@:%@", @"224.2.2.2", @"5111"];
        multicastSocket = -1;
        return;
    }
    
    CFSocketContext ctxt;
    
    ctxt.version = 0;
    ctxt.info = (__bridge void *) self;
    ctxt.retain = CFRetain;
    ctxt.release = CFRelease;
    ctxt.copyDescription = NULL;
    
    CFSocketRef connection = CFSocketCreateWithNative(kCFAllocatorDefault,
                                                      multicastSocket,
                                                      kCFSocketDataCallBack,
                                                      handleNetworkData,
                                                      &ctxt);
    
    runLoopSource = CFSocketCreateRunLoopSource(kCFAllocatorDefault, connection, 0);
    
    CFRunLoopAddSource(CFRunLoopGetCurrent(), runLoopSource, kCFRunLoopDefaultMode);
    
    [self debugTextAppend:@"Multicast Socket started at %@:%@", @"224.2.2.2", @"5111"];

}

- (void)refreshMulticast {
    if (multicastSocket < 0) {
        [self prepareMulticast];
    }
    else {
        [self debugTextAppend:@"Multicast Socket refreshing"];
        CFRunLoopRemoveSource(CFRunLoopGetCurrent(), runLoopSource, kCFRunLoopDefaultMode);
        close(multicastSocket);
        [self debugTextAppend:@"Multicast Socket closed"];
        [self prepareMulticast];
    }
}

- (void)startMyTimer {
    NSTimer *myTimer;
    myTimer = [NSTimer scheduledTimerWithTimeInterval:10 target:self selector:@selector(timerTask:) userInfo:nil repeats:YES];
}

-(void)timerTask:(NSTimer*)timer{
    CGSize size = [_debugText contentSize];
    if(size.height > 500) {
        [_debugText setText:@""];
    }
}

- (void)sendBytes:(NSData *)byteArr
{
    if(multicastSocket < 0)
    {
        NSLog(@"sendBytes failed as multicastSocket < 0");
        return;
    }

    int len = (int)[byteArr length];
    
    struct sockaddr_in recver_addr;
    memset(&recver_addr, 0, sizeof(recver_addr));
    recver_addr.sin_family = AF_INET;
    recver_addr.sin_port = htons(5111);
    recver_addr.sin_addr.s_addr = inet_addr("224.2.2.2");
    int ret = sendto(multicastSocket, [byteArr bytes], [byteArr length], 0, (struct sockaddr*)&recver_addr, sizeof(recver_addr));
    
    NSLog(@"sendBytes function: ret=%02X, len=%02X, multicastSocket=%02X", ret, len, multicastSocket);
    
    if (ret == len) {
        if (len > 0) {
            NSMutableString *hexStr = [NSMutableString stringWithCapacity:len];
            for (int i=0; i < len; i++) {
                [hexStr appendFormat: @"%02X ", ((Byte *)[byteArr bytes])[i]];
            }
            
            [self debugTextAppend:@"Data sent: %@", hexStr];
            NSLog(@"Data sent: %@", hexStr);
        }
    }
    else{
        [self refreshMulticast];

        // only retry once
        
        int ret = sendto(multicastSocket, [byteArr bytes], [byteArr length], 0, (struct sockaddr*)&recver_addr, sizeof(recver_addr));
        
        NSLog(@"sendBytes function: ret=%02X, len=%02X, multicastSocket=%02X", ret, len, multicastSocket);
        
        if (ret == len) {
            if (len > 0) {
                NSMutableString *hexStr = [NSMutableString stringWithCapacity:len];
                for (int i=0; i < len; i++) {
                    [hexStr appendFormat: @"%02X ", ((Byte *)[byteArr bytes])[i]];
                }
                
                [self debugTextAppend:@"Data sent: %@", hexStr];
                NSLog(@"Data sent: %@", hexStr);
            }
        }
    }
}

- (void)sendCommand:(const Byte)cmdIn macAddress:(const Byte *)mac
{
    Byte *data = malloc(PACKET_LENGTH);
    memset(data, 0, PACKET_LENGTH);
    data[0] = 0x55;
    for (int i = 0; i < 6; i++) {
        data[i+1] = mac[i];
    }
    data[7] = cmdIn;
    int crc = 0;
    for (int i = 0; i < PACKET_LENGTH - 1; i++) {
        crc += (data[i] & 0xFF);
    }
    crc = (crc % 256) & 0xFF;
    data[PACKET_LENGTH - 1] = crc;

    NSData *cmd = [NSData dataWithBytes:data length:PACKET_LENGTH];

    if (data) {
        free(data);
    }
    
    [self sendBytes:cmd];
}


- (void)setTemperature:(int)temp
{
}

- (void)setHumidity:(int)humidity
{
}

- (void)remoteControlCmd:(NSString *)cmd
{
    NSLog(cmd);
    
    const Byte* mac = [self getSelectedMacAddress];
    
    if ([cmd isEqualToString:@"Power"]) {
        [self sendCommand:cmdDVDPower macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Mute"]) {
        [self sendCommand:cmdDVDMute macAddress:mac];
    }
    else if ([cmd isEqualToString:@"1"]) {
        [self sendCommand:cmdDVDNumber1 macAddress:mac];
    }
    else if ([cmd isEqualToString:@"2"]) {
        [self sendCommand:cmdDVDNumber2 macAddress:mac];
    }
    else if ([cmd isEqualToString:@"3"]) {
        [self sendCommand:cmdDVDNumber3 macAddress:mac];
    }
    else if ([cmd isEqualToString:@"4"]) {
        [self sendCommand:cmdDVDNumber4 macAddress:mac];
    }
    else if ([cmd isEqualToString:@"5"]) {
        [self sendCommand:cmdDVDNumber5 macAddress:mac];
    }
    else if ([cmd isEqualToString:@"6"]) {
        [self sendCommand:cmdDVDNumber6 macAddress:mac];
    }
    else if ([cmd isEqualToString:@"7"]) {
        [self sendCommand:cmdDVDNumber7 macAddress:mac];
    }
    else if ([cmd isEqualToString:@"8"]) {
        [self sendCommand:cmdDVDNumber8 macAddress:mac];
    }
    else if ([cmd isEqualToString:@"9"]) {
        [self sendCommand:cmdDVDNumber9 macAddress:mac];
    }
    else if ([cmd isEqualToString:@"0"]) {
        [self sendCommand:cmdDVDNumber0 macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Set"]) {
        [self sendCommand:cmdDVDSet macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Back"]) {
        [self sendCommand:cmdDVDBack macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Up"]) {
        [self sendCommand:cmdDVDUp macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Down"]) {
        [self sendCommand:cmdDVDDown macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Left"]) {
        [self sendCommand:cmdDVDLeft macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Right"]) {
        [self sendCommand:cmdDVDRight macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Ok"]) {
        [self sendCommand:cmdDVDOk macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Play"]) {
        [self sendCommand:cmdDVDPlay macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Pause"]) {
        [self sendCommand:cmdDVDPause macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Stop"]) {
        [self sendCommand:cmdDVDStop macAddress:mac];
    }
    else if ([cmd isEqualToString:@"FF"]) {
        [self sendCommand:cmdDVDFF macAddress:mac];
    }
    else if ([cmd isEqualToString:@"FB"]) {
        [self sendCommand:cmdDVDFB macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Prev"]) {
        [self sendCommand:cmdDVDPrev macAddress:mac];
    }
    else if ([cmd isEqualToString:@"Net"]) {
        [self sendCommand:cmdDVDNext macAddress:mac];
    }
}

- (void)debugTextAppend:(NSString *)format, ...
{
    va_list vl;
    va_start(vl, format);
    NSString *str = [[NSString alloc] initWithFormat:format arguments:vl];
    va_end(vl);

    NSString *newDebugText = [NSString stringWithFormat:@"%@\n%@", [_debugText text], str];
    
    [_debugText setText:newDebugText];
}

- (NSString *)getLocalIpAddressString {
    
    NSString *address = @"error";
    struct ifaddrs *interfaces = NULL;
    struct ifaddrs *temp_addr = NULL;
    int success = 0;
    // retrieve the current interfaces - returns 0 on success
    success = getifaddrs(&interfaces);
    if (success == 0) {
        // Loop through linked list of interfaces
        temp_addr = interfaces;
        while(temp_addr != NULL) {
            if(temp_addr->ifa_addr->sa_family == AF_INET) {
                // Check if interface is en0 which is the wifi connection on the iPhone
                if([[NSString stringWithUTF8String:temp_addr->ifa_name] isEqualToString:@"en0"]) {
                    // Get NSString from C String
                    address = [NSString stringWithUTF8String:inet_ntoa(((struct sockaddr_in *)temp_addr->ifa_addr)->sin_addr)];
                    
                }
                
            }
            
            temp_addr = temp_addr->ifa_next;
        }
    }
    // Free memory
    freeifaddrs(interfaces);
    return address;
    
}

- (IBAction)onClickSwitchLightBulb:(UIButton *)sender {
    if (!isLightOn) {
        [self sendCommand:cmdTurnOnLED macAddress:[self getSelectedMacAddress]];
    } else {
        [self sendCommand:cmdTurnOffLED macAddress:[self getSelectedMacAddress]];
    }
}

- (IBAction)onClickBtnGetTemperature:(UIButton *)sender {
    [self sendCommand:cmdGetTemperature macAddress:[self getSelectedMacAddress]];
}

- (IBAction)onClickBtnGetHumidity:(UIButton *)sender {
    [self sendCommand:cmdGetHumidity macAddress:[self getSelectedMacAddress]];
}

- (IBAction)onClickBtnDVDRemote:(UIButton *)sender {
}

- (BOOL)textFieldShouldReturn:(UITextField *)textField {
    [textField resignFirstResponder];
    return NO;
}

- (NSString*) byteToHexStringWithout0x:(const Byte *)data {

    int len = sizeof(data);
    NSMutableString *hexStr = [NSMutableString stringWithCapacity:len];
    
    for (int i = 0; i < len; i++) {
        [hexStr appendFormat: @"%02X ", data[i]];
    }
    return hexStr;
}

- (NSString*) nsdataToHexStringWithout0x:(const NSData *)data {
    
    int len = [data length];
    NSMutableString *hexStr = [NSMutableString stringWithCapacity:len];
    
    for (int i = 0; i < len; i++) {
        [hexStr appendFormat: @"%02X ", ((Byte *)[data bytes])[i]];
    }
    return hexStr;
}

- (bool) IsSameMacAddress:(const Byte *)mac1 MAC2:(const Byte *)mac2 {
    for (int i = 0; i < 6; i++) {
        if(mac1[i] != mac2[i]) {
            return false;
        }
    }
    return true;
}

- (void) updateMacPicker:(const Byte *)mac {
    arrayMac = [NSMutableArray arrayWithCapacity:1];
    
    NSData *tmp = [NSData dataWithBytes:mac length:6];
    
    [arrayMac addObject:tmp];
    
    self.pickerMac.dataSource = self;
    self.pickerMac.delegate = self;
}

- (void) appendMacPicker:(const Byte *)mac {
   
    NSData *tmp = [NSData dataWithBytes:mac length:6];
    int count = arrayMac.count;
    bool macStored = false;
    for (int i = 0; i < count; i++) {
        if([self IsSameMacAddress:mac MAC2:[[arrayMac objectAtIndex:i] bytes]]) {
            macStored = true;
            break;
        }
    }
    if(macStored == false) {
        [arrayMac addObject:tmp];
        [self.pickerMac reloadComponent:0];
    }
}

- (NSInteger)numberOfComponentsInPickerView:(UIPickerView*)pickerView
{
    return 1;
}

- (NSInteger)pickerView:(UIPickerView *)pickerView numberOfRowsInComponent:(NSInteger)component
{
    return arrayMac.count;
}

- (NSString*)pickerView:(UIPickerView *)pickerView
             titleForRow:(NSInteger)row forComponent:(NSInteger)component
{
    return [self nsdataToHexStringWithout0x:[arrayMac objectAtIndex:row]];
}

- (void)pickerView:(UIPickerView *)pickerView didSelectRow:(NSInteger)row inComponent:(NSInteger)component
{
    [self sendCommand:cmdGetAll macAddress:[[arrayMac objectAtIndex:row] bytes]];

    /*UIAlertView* alert = [[UIAlertView alloc]
                          initWithTitle:@"提示"
                          message:[NSString stringWithFormat:@"你选中的是：%@", [self nsdataToHexStringWithout0x:[arrayMac objectAtIndex:row]]]
                          delegate:nil
                          cancelButtonTitle:@"确定"
                          otherButtonTitles:nil];
    [alert show];*/
}

- (const Byte *)getSelectedMacAddress {
    NSInteger row = [self.pickerMac selectedRowInComponent:0];
    NSData *mac = [arrayMac objectAtIndex:row];
    return [mac bytes];
}

@end
