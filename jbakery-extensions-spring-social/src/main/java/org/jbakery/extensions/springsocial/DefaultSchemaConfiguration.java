package org.jbakery.extensions.springsocial;

public final class DefaultSchemaConfiguration
	implements SchemaConfiguration
{
	@Override
	public String getAccessTokenColumnName()
	{
		return "accessToken";
	}

	@Override
	public String getDisplayNameColumnName()
	{
		return "displayName";
	}

	@Override
	public String getExpireTimeColumnName()
	{
		return "expireTime";
	}

	@Override
	public String getImageUrlColumnName()
	{
		return "imageUrl";
	}

	@Override
	public String getProfileUrlColumnName()
	{
		return "profileUrl";
	}

	@Override
	public String getProviderIdColumnName()
	{
		return "providerId";
	}

	@Override
	public String getProviderUserIdColumnName()
	{
		return "providerUserId";
	}

	@Override
	public String getRankColumnName()
	{
		return "rank";
	}

	@Override
	public String getRefreshTokenColumnName()
	{
		return "refreshToken";
	}

	@Override
	public String getSecretColumnName()
	{
		return "secret";
	}

	@Override
	public String getTableName()
	{
		return "UserConnection";
	}

	@Override
	public String getUserIdColumnName()
	{
		return "userId";
	}
}
