#[cfg(test)]
mod tests {
    #[test]
    fn it_works() {
        assert_eq!(2 + 2, 4);
    }
    
    #[test]
    fn it_fails() {
        use std::{thread, time};
        thread::sleep(time::Duration::from_secs(5));
        assert_eq!(2 + 2, 5);
    }
}
