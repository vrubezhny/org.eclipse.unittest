#[cfg(test)]
mod tests1 {
    #[test]
    fn it_works() {
        assert_eq!(2 + 2, 4);
    }
    
    #[test]
    fn it_fails_on_assert_eq() {
        use std::{thread, time};
        thread::sleep(time::Duration::from_secs(5));
        assert_eq!(2 + 2, 5);
    }

    #[test]
    fn it_fails_on_assert_ne() {
        use std::{thread, time};
        thread::sleep(time::Duration::from_secs(5));
		assert_ne!(2 + 2, 4);
    }

    #[test]
    fn it_fails_on_panic() {
        panic!("Make this test fail");
    }

    #[test]
    fn it_fails_on_assert() {
		let test_variable = false;
		assert!(test_variable);
    }

    #[test]
    fn it_fails_on_assert_with_message() {
		let test_variable = false;
		assert!(test_variable, "The value of `test_variable` is FALSE!");
    }

	pub struct Guess {
	    value: i32,
	}
	
	impl Guess {
	    pub fn new(value: i32) -> Guess {
	        if value < 1 {
	            panic!("Guess value must be less than or equal to 100, got {}.", value);
	        } else if  value > 100 {
	            panic!("Guess value must be greater than or equal to 1, got {}.", value);
	        }
	
	        Guess { value }
	    }
	}
	
    #[test]
    #[should_panic(expected = "Guess value must be greater than or equal to 1")]
    fn fails_on_should_panic_less_than_1() {
        Guess::new(0);
    }

    #[test]
    #[should_panic(expected = "Guess value must be less than or equal to 100")]
    fn it_it_fails_on_should_panic_greater_than_100() {
        Guess::new(2000);
    }

    #[test]
   	#[deny(clippy::eq_op)]
    fn it_works_again() -> Result<(), String> {
		let a = 2 + 2;
        if a == 4 {
            Ok(())
        } else {
            Err(String::from("two plus two does not equal four"))
        }
    }

    #[test]
   	#[deny(clippy::eq_op)]
    fn it_fails_on_result() -> Result<(), String> {
		let a = 2 + 2;
        if a == 5 {
            Ok(())
        } else {
            Err(String::from("two plus two does not equal four"))
        }
    }

	fn prints_and_returns_10(a: i32) -> i32 {
	    println!("I got the value {}", a);
	    10
	}

    #[test]
    fn this_test_will_pass() {
        let value = prints_and_returns_10(4);
        assert_eq!(10, value);
    }

    #[test]
    fn this_test_will_fail() {
        let value = prints_and_returns_10(8);
        assert_eq!(5, value);
    }
}
mod tests2 {
    #[test]
    fn it_works() {
        assert_eq!(2 + 2, 4);
    }
    
    #[test]
    fn it_fails_on_assert_eq() {
        use std::{thread, time};
        thread::sleep(time::Duration::from_secs(5));
        assert_eq!(2 + 2, 5);
    }

    #[test]
    fn it_fails_on_assert_ne() {
        use std::{thread, time};
        thread::sleep(time::Duration::from_secs(5));
		assert_ne!(2 + 2, 4);
    }

    #[test]
    fn it_fails_on_panic() {
        panic!("Make this test fail");
    }

    #[test]
    fn it_fails_on_assert() {
		let test_variable = false;
		assert!(test_variable);
    }

    #[test]
    fn it_fails_on_assert_with_message() {
		let test_variable = false;
		assert!(test_variable, "The value of `test_variable` is FALSE!");
    }

	pub struct Guess {
	    value: i32,
	}
	
	impl Guess {
	    pub fn new(value: i32) -> Guess {
	        if value < 1 {
	            panic!("Guess value must be less than or equal to 100, got {}.", value);
	        } else if  value > 100 {
	            panic!("Guess value must be greater than or equal to 1, got {}.", value);
	        }
	
	        Guess { value }
	    }
	}
	
    #[test]
    #[should_panic(expected = "Guess value must be greater than or equal to 1")]
    fn fails_on_should_panic_less_than_1() {
        Guess::new(0);
    }

    #[test]
    #[should_panic(expected = "Guess value must be less than or equal to 100")]
    fn it_it_fails_on_should_panic_greater_than_100() {
        Guess::new(2000);
    }

    #[test]
   	#[deny(clippy::eq_op)]
    fn it_works_again() -> Result<(), String> {
		let a = 2 + 2;
        if a == 4 {
            Ok(())
        } else {
            Err(String::from("two plus two does not equal four"))
        }
    }

    #[test]
   	#[deny(clippy::eq_op)]
    fn it_fails_on_result() -> Result<(), String> {
		let a = 2 + 2;
        if a == 5 {
            Ok(())
        } else {
            Err(String::from("two plus two does not equal four"))
        }
    }

	fn prints_and_returns_10(a: i32) -> i32 {
	    println!("I got the value {}", a);
	    10
	}

    #[test]
    fn this_test_will_pass() {
        let value = prints_and_returns_10(4);
        assert_eq!(10, value);
    }

    #[test]
    fn this_test_will_fail() {
        let value = prints_and_returns_10(8);
        assert_eq!(5, value);
    }
}
