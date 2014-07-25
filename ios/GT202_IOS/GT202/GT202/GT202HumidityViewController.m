//
//  GT202HumidityViewController.m
//  GT202
//
//  Created by Edmond Leung on 18/5/14.
//  Copyright (c) 2014 Arrow. All rights reserved.
//

#import "GT202HumidityViewController.h"

@interface GT202HumidityViewController ()
@property (weak, nonatomic) IBOutlet UILabel *txtCurrentHumidity;
@property (weak, nonatomic) IBOutlet UILabel *txtTargetHumidity;
@property (weak, nonatomic) IBOutlet UIStepper *targetHumidityStepper;

@end

@implementation GT202HumidityViewController

- (id)initWithNibName:(NSString *)nibNameOrNil bundle:(NSBundle *)nibBundleOrNil
{
    self = [super initWithNibName:nibNameOrNil bundle:nibBundleOrNil];
    if (self) {
        // Custom initialization
    }
    return self;
}

- (void)viewDidLoad
{
    [super viewDidLoad];
    // Do any additional setup after loading the view.
    [[self txtCurrentHumidity] setText:[[_home textViewHumidity] text]];
    
    [[self targetHumidityStepper] setValue:[[[self txtTargetHumidity] text] intValue]];
    
}

- (void)didReceiveMemoryWarning
{
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

/*
#pragma mark - Navigation

// In a storyboard-based application, you will often want to do a little preparation before navigation
- (void)prepareForSegue:(UIStoryboardSegue *)segue sender:(id)sender
{
    // Get the new view controller using [segue destinationViewController].
    // Pass the selected object to the new view controller.
}
*/

- (IBAction)onClickBtnChangeHumidity:(UIStepper *)sender {
    int stepValue = sender.value;
    NSString *nssHum = [NSString stringWithFormat:@"%d%%", stepValue];
    
    [[self txtTargetHumidity] setText: nssHum];
    
    [_home setHumidity:stepValue];
}


@end
