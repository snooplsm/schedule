package com.happytap.schedule.service;

import org.jibble.pircbot.PircBot;

public class ChatService extends PircBot {
	
	public ChatService(String nick) {
		setName(nick);
	}
	
}
