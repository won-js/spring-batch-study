package study.batch.week3;

import org.springframework.batch.item.file.mapping.FieldSetMapper;
import org.springframework.batch.item.file.transform.FieldSet;

public class PlayerFieldSetMapper implements FieldSetMapper<Player> {
    @Override
    public Player mapFieldSet(FieldSet fs) {
        if (fs == null) return null;

        Player player = new Player();

        player.setNo(fs.readLong(0));
        player.setName(fs.readString(1));
        player.setAge(fs.readInt(2));

        return player;
    }
}
