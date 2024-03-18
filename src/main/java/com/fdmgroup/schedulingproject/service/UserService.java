package com.fdmgroup.schedulingproject.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fdmgroup.schedulingproject.exception.UserNotFoundException;
import com.fdmgroup.schedulingproject.model.User;
import com.fdmgroup.schedulingproject.repository.UserRepository;

@Service
public class UserService {
	@Autowired
	protected UserRepository userRepo;

	public void setUserRepository(UserRepository userRepo) {
		this.userRepo = userRepo;
	}

	public User findUser(String username) throws UserNotFoundException {
		return userRepo.findByUsername(username).orElseThrow(UserNotFoundException::new);
	}

}
