package model.requests;

import lombok.Data;

@Data
public class JoinGameInput {
    public String groupId;
    public String nick;
    public int age = 20;
}