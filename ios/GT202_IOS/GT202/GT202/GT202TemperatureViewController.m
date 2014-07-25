//
//  GT202TemperatureViewController.m
//  GT202
//
//  Created by Edmond Leung on 17/5/14.
//  Copyright (c) 2014 Arrow. All rights reserved.
//

#import "GT202TemperatureViewController.h"

@interface GT202TemperatureViewController ()
@property (weak, nonatomic) IBOutlet UILabel *txtCurrentTemperature;
@property (weak, nonatomic) IBOutlet UILabel *txtTargetTemperature;
@property (weak, nonatomic) IBOutlet UIStepper *targetTemperatureStepper;

@end

@implementation GT202TemperatureViewController
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
    [[self txtCurrentTemperature] setText:[[_home textViewTemperature] text]];
    
    [[self targetTemperatureStepper] setValue:[[[self txtTargetTemperature] text] intValue]];
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

- (IBAction)onClickBtnChangeTemperature:(UIStepper *)sender {
    int stepValue = sender.value;
    NSString *nssTemp = [NSString stringWithFormat:@"%dºC", stepValue];
    
    [[self txtTargetTemperature] setText: nssTemp];
    
    [_home setTemperature:stepValue];
}


@end
