-- Full-text search on message content (InnoDB FULLTEXT)

CREATE FULLTEXT INDEX ft_messages_content ON messages(content);
