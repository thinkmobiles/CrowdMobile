package com.kes;

import com.kes.model.PhotoComment;
import com.kes.model.User;

/**
 * Created by gadza on 2015.03.13..
 */
public class ResultWrapper {

    protected enum ActionType {Like , Report, Delete, MarkAsPrivate, MarkAsRead};

    public Exception exception;
    protected ActionType actionType;
    protected boolean suppressError = false;
    protected int questionID;
    protected int commentID;
    protected boolean isPrivate;
    protected User user;
    protected PhotoComment photoComment;
}
