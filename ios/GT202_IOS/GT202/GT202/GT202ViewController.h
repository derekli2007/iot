//
//  GT202ViewController.h
//  GT202
//
//  Created by Edmond Leung on 12/5/14.
//  Copyright (c) 2014 Arrow. All rights reserved.
//

#import <UIKit/UIKit.h>

NSInputStream *inputStream;
NSOutputStream *outputStream;

@interface GT202ViewController : UIViewController <NSStreamDelegate, UITextFieldDelegate>
@property (weak, nonatomic) IBOutlet UILabel *textViewTemperature;
@property (weak, nonatomic) IBOutlet UILabel *textViewHumidity;
@property (weak, nonatomic) IBOutlet UIPickerView *pickerMac;

- (IBAction)unwindToHome:(UIStoryboardSegue *)segue;

- (void)setTemperature:(int)temp;
- (void)setHumidity:(int)humidity;
- (void)remoteControlCmd:(NSString *)cmd;
- (void)refreshMulticast;

@end

GT202ViewController *globalSelf;