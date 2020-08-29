/*
 * gtestx_test.cpp
 *
 *  Created on: Aug 7, 2020
 *      Author: jeremy
 */

#include "gtestx.h"

#include "gtest/gtest.h"

TEST(googleTestTest, Test1) {
	ASSERT_EQ(100, 100); 	// Success
}

TEST(googleTestTest, Test2) {
	ASSERT_EQ(100, 101); 	// Fail
}

TEST(googleTestTest, Test3) {
	ASSERT_EQ(100, 100); 	// Success
}
